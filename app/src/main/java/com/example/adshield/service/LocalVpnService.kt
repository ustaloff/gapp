package com.example.adshield.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.adshield.MainActivity
import com.example.adshield.data.VpnStats
import com.example.adshield.net.DnsProxy
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer

class LocalVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val vpnScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val vpnDnsServer = "10.0.0.3"

    companion object {
        const val ACTION_START = "com.example.adshield.service.START_VPN"
        const val ACTION_STOP = "com.example.adshield.service.STOP_VPN"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            startVpn()
        } else {
            stopVpn()
        }
        return START_NOT_STICKY
    }

    private fun startVpn() {
        if (VpnStats.isRunning.value) return

        Log.i("LocalVpnService", "Starting VPN...")
        val establishedInterface = setupVpnInterface()
        if (establishedInterface == null) {
            Log.e("LocalVpnService", "Failed to establish VPN interface.")
            stopSelf()
            return
        }
        this.vpnInterface = establishedInterface

        VpnStats.setStatus(true)
        startForegroundWithNotification()
        vpnScope.launch { runPacketLoop(establishedInterface) }
        Log.i("LocalVpnService", "VPN has been started.")
    }

    private fun stopVpn() {
        if (!VpnStats.isRunning.value) return
        Log.i("LocalVpnService", "Stopping VPN...")
        VpnStats.setStatus(false)
        vpnScope.cancel()
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: IOException) {
            Log.e("LocalVpnService", "Error closing VPN interface", e)
        }
        stopForeground(true)
        stopSelf()
    }

    private fun setupVpnInterface(): ParcelFileDescriptor? {
        return try {
            Builder().apply {
                setSession("AdShield")
                addAddress("10.0.0.2", 32)
                addDnsServer(vpnDnsServer)
                addRoute(vpnDnsServer, 32)
                
                // 1. Always exclude AdShield itself (Network Loop prevention)
                addDisallowedApplication(packageName)
                
                // 2. Exclude user-selected apps (Split Tunneling)
                val prefs = com.example.adshield.data.AppPreferences(this@LocalVpnService)
                val excludedApps = prefs.getExcludedApps()
                Log.i("LocalVpnService", "Excluding ${excludedApps.size} user-selected apps from VPN")
                
                excludedApps.forEach { excludedPackage ->
                    try {
                        addDisallowedApplication(excludedPackage)
                    } catch (e: Exception) {
                        Log.e("LocalVpnService", "Failed to exclude app: $excludedPackage", e)
                    }
                }

                setBlocking(true)
            }.establish()
        } catch (e: Exception) {
            Log.e("LocalVpnService", "Error setting up VPN builder", e)
            null
        }
    }

    private suspend fun runPacketLoop(vpnInterface: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        Log.d("LocalVpnService", "Packet loop starting.")
        try {
            FileInputStream(vpnInterface.fileDescriptor).use { inputStream ->
                FileOutputStream(vpnInterface.fileDescriptor).use { outputStream ->
                    // Allocate buffer. MTU is usually 1500, but 32767 is safe for safety.
                    val packet = ByteBuffer.allocate(32767)
                    val dnsProxy = DnsProxy(this@LocalVpnService)
                    
                    while (isActive) {
                        // Read blocking
                        val length = inputStream.read(packet.array())
                        
                        if (length > 0) {
                            packet.limit(length)
                            
                            // Check for IPv4 UDP packet to avoid wasting threads on garbage
                            // (Though we only route proper traffic, it's good safety)
                             val firstByte = packet.get(0).toInt()
                             // Basic check: Version 4
                             if ((firstByte shr 4) == 4) {
                                 // Check Protocol (Byte 9)
                                 val protocol = packet.get(9).toInt()
                                 if (protocol == 17) { // UDP
                                     // 1. Copy data
                                    val packetData = ByteArray(length)
                                    System.arraycopy(packet.array(), 0, packetData, 0, length)
                                    val packetCopy = ByteBuffer.wrap(packetData)
                                    
                                    // 2. Launch coroutine
                                    launch {
                                        try {
                                            val response = dnsProxy.handleDnsRequest(packetCopy)
                                            if (response != null) {
                                                synchronized(outputStream) {
                                                    outputStream.write(response.array(), 0, response.limit())
                                                }
                                            } else {
                                                Log.w("LocalVpnService", "DNS Proxy returned null")
                                            }
                                        } catch (e: Exception) {
                                             Log.e("LocalVpnService", "Error processing packet", e)
                                        }
                                    }
                                 }
                             }
                            
                            packet.clear()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.w("LocalVpnService", "Packet loop closed.", e)
        } catch (e: Exception) {
            Log.e("LocalVpnService", "Packet loop critical error", e)
        }
        Log.d("LocalVpnService", "Packet loop finished.")
    }

    private fun startForegroundWithNotification() {
        val channelId = "adshield_vpn_status"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "AdShield Status", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)

        val notification = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(this, channelId)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(this)
        })
            .setContentTitle("AdShield is protecting you")
            .setContentText("Filtering DNS traffic")
            .setSmallIcon(android.R.drawable.ic_secure)
            .setContentIntent(pendingIntent)
            .build()

        // THIS IS THE CRITICAL FIX
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
