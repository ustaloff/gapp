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
import java.io.InterruptedIOException
import android.net.ConnectivityManager
import java.net.InetSocketAddress
import java.net.InetAddress
import java.nio.ByteBuffer

class LocalVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private val vpnScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val outputChannel = kotlinx.coroutines.channels.Channel<ByteBuffer>(capacity = 100)
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
        
        // Start Reader and Writer
        vpnScope.launch { runPacketLoop(establishedInterface) }
        vpnScope.launch { runWriterLoop(establishedInterface) }
        
        Log.i("LocalVpnService", "VPN has been started.")
    }

    private fun stopVpn() {
        if (!VpnStats.isRunning.value) return
        Log.i("LocalVpnService", "Stopping VPN...")
        VpnStats.setStatus(false)
        
        // Cancel scope first to notify loops
        vpnScope.cancel()
        
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: IOException) {
            Log.e("LocalVpnService", "Error closing VPN interface", e)
        }
        
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun setupVpnInterface(): ParcelFileDescriptor? {
        return try {
            Builder().apply {
                setSession("AdShield")
                setMtu(1500)
                
                addAddress("10.0.0.2", 24)
                addDnsServer(vpnDnsServer)
                addRoute(vpnDnsServer, 32)
                
                // NEW: Add IPv6 DNS support
                val vpnDnsServerV6 = "fd00::3"
                addAddress("fd00::2", 64)
                addDnsServer(vpnDnsServerV6)
                addRoute(vpnDnsServerV6, 128)
                
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

    private suspend fun runWriterLoop(vpnInterface: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        try {
            FileOutputStream(vpnInterface.fileDescriptor).use { outputStream ->
                for (buffer in outputChannel) {
                    if (!isActive) break
                    try {
                        outputStream.write(buffer.array(), 0, buffer.limit())
                    } catch (e: Exception) {
                        if (isActive) Log.e("LocalVpnService", "Write failed", e)
                    }
                }
            }
        } catch (e: CancellationException) {
            Log.i("LocalVpnService", "Writer loop cancelled normally")
        } catch (e: Exception) {
            if (isActive) Log.e("LocalVpnService", "Writer loop crashed", e)
        }
    }

    private suspend fun runPacketLoop(vpnInterface: ParcelFileDescriptor) = withContext(Dispatchers.IO) {
        Log.d("LocalVpnService", "Packet loop starting.")
        try {
            FileInputStream(vpnInterface.fileDescriptor).use { inputStream ->
                // Allocate buffer. MTU is usually 1500, but 32767 is safe for safety.
                val packet = ByteBuffer.allocate(32767)
                val dnsProxy = DnsProxy(this@LocalVpnService)
                
                while (isActive) {
                    val length = inputStream.read(packet.array())
                    
                    if (length > 0) {
                        packet.limit(length)
                        
                        val firstByte = packet.get(0).toInt()
                        val version = (firstByte shr 4) and 0x0F
                        var protocol: Int = -1
                        var ipHeaderSize = 0
                        
                        if (version == 4) {
                            protocol = packet.get(9).toInt() and 0xFF
                            ipHeaderSize = (firstByte and 0x0F) * 4
                        } else if (version == 6) {
                            protocol = packet.get(6).toInt() and 0xFF
                            ipHeaderSize = 40
                        }

                        if (protocol == 17) { // UDP
                            val sourcePort = packet.getShort(ipHeaderSize).toInt() and 0xFFFF
                            val destPort = packet.getShort(ipHeaderSize + 2).toInt() and 0xFFFF
                            
                            if (destPort == 53) {
                                val appName = getAppNameForPacket(packet, sourcePort, version)
                                
                                val packetData = ByteArray(length)
                                System.arraycopy(packet.array(), 0, packetData, 0, length)
                                val packetCopy = ByteBuffer.wrap(packetData)
                                
                                launch {
                                    try {
                                        val response = dnsProxy.handleDnsRequest(packetCopy, appName)
                                        if (response != null) {
                                            outputChannel.send(response)
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
        } catch (e: InterruptedIOException) {
            Log.i("LocalVpnService", "Packet loop interrupted (normal shutdown)")
        } catch (e: CancellationException) {
            Log.i("LocalVpnService", "Packet loop cancelled (normal shutdown)")
        } catch (e: IOException) {
            if (isActive) Log.w("LocalVpnService", "Packet loop closed unexpected.", e)
        } catch (e: Exception) {
            if (isActive) Log.e("LocalVpnService", "Packet loop critical error", e)
        }
        outputChannel.close()
        Log.d("LocalVpnService", "Packet loop finished.")
    }

    private fun getAppNameForPacket(packet: ByteBuffer, sourcePort: Int, ipVersion: Int): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val srcAddr: InetAddress
            val dstAddr: InetAddress
            
            if (ipVersion == 4) {
                val srcBytes = ByteArray(4)
                val dstBytes = ByteArray(4)
                packet.position(12); packet.get(srcBytes)
                packet.position(16); packet.get(dstBytes)
                srcAddr = InetAddress.getByAddress(srcBytes)
                dstAddr = InetAddress.getByAddress(dstBytes)
            } else {
                val srcBytes = ByteArray(16)
                val dstBytes = ByteArray(16)
                packet.position(8); packet.get(srcBytes)
                packet.position(24); packet.get(dstBytes)
                srcAddr = InetAddress.getByAddress(srcBytes)
                dstAddr = InetAddress.getByAddress(dstBytes)
            }

            val uid = cm.getConnectionOwnerUid(
                17, // UDP
                InetSocketAddress(srcAddr, sourcePort),
                InetSocketAddress(dstAddr, 53)
            )

            if (uid != -1) {
                val pm = packageManager
                val packages = pm.getPackagesForUid(uid)
                if (!packages.isNullOrEmpty()) {
                    val ai = pm.getApplicationInfo(packages[0], 0)
                    pm.getApplicationLabel(ai).toString()
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
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
