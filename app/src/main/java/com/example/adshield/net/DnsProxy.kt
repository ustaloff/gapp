package com.example.adshield.net

import android.net.VpnService
import android.util.Log
import com.example.adshield.data.VpnStats
import com.example.adshield.filter.FilterEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class DnsProxy(private val vpnService: VpnService) {

    private val primaryDns = "8.8.8.8"
    private val secondaryDns = "1.1.1.1"
    private val upstreamPort = 53
    
    // Socket Pool for high-performance reuse
    private val socketPool = mutableListOf<DatagramSocket>()
    private val poolMutex = Mutex()
    private val MAX_POOL_SIZE = 10

    private suspend fun getPooledSocket(): DatagramSocket {
        poolMutex.withLock {
            if (socketPool.isNotEmpty()) {
                return socketPool.removeAt(0)
            }
        }
        val socket = DatagramSocket()
        vpnService.protect(socket)
        return socket
    }

    private suspend fun returnToPool(socket: DatagramSocket) {
        poolMutex.withLock {
            if (socketPool.size < MAX_POOL_SIZE) {
                socketPool.add(socket)
            } else {
                socket.close()
            }
        }
    }

    // Privacy Feature: DoH is enabled by default
    var useDoh = true

    private suspend fun resolveViaUdp(dnsQuery: ByteArray, domain: String, transactionId: Short): ByteArray? {
        // Try Primary then Secondary
        return resolveViaSingleUdp(dnsQuery, domain, transactionId, primaryDns) 
            ?: resolveViaSingleUdp(dnsQuery, domain, transactionId, secondaryDns)
    }

    private suspend fun resolveViaSingleUdp(dnsQuery: ByteArray, domain: String, transactionId: Short, upstreamDns: String): ByteArray? {
        val socket = getPooledSocket()
        return try {
            val upstreamAddr = InetAddress.getByName(upstreamDns)
            val outPacket = DatagramPacket(dnsQuery, dnsQuery.size, upstreamAddr, upstreamPort)
            
            withContext(Dispatchers.IO) {
                socket.send(outPacket)
                socket.soTimeout = 2000
                
                val responseBuffer = ByteArray(1500)
                val inPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                
                // Read until we get the matching transaction ID or timeout
                while (true) {
                    socket.receive(inPacket)
                    if (inPacket.length >= 2) {
                        val responseId = ((inPacket.data[0].toInt() and 0xFF) shl 8 or (inPacket.data[1].toInt() and 0xFF)).toShort()
                        if (responseId == transactionId) {
                            val response = inPacket.data.sliceArray(0 until inPacket.length)
                            Log.i("DnsProxy", "Resolved (UDP $upstreamDns): $domain")
                            return@withContext response
                        }
                    }
                }
                null
            }
        } catch (e: Exception) {
            Log.v("DnsProxy", "UDP $upstreamDns error for $domain: ${e.message}")
            null
        } finally {
            returnToPool(socket)
        }
    }

    suspend fun handleDnsRequest(requestPacket: ByteBuffer, appName: String? = null): ByteBuffer? {
        try {
            val ipHeaderSize = PacketUtils.getIpHeaderLength(requestPacket)
            if (ipHeaderSize < 20) return null
            
            val payloadOffset = ipHeaderSize + PacketUtils.UDP_HEADER_SIZE
            val payloadSize = requestPacket.limit() - payloadOffset
            if (payloadSize <= 0) return null

            val dnsQuery = ByteArray(payloadSize)
            requestPacket.position(payloadOffset)
            requestPacket.get(dnsQuery)

            val dnsMessage = DnsMessage(dnsQuery)
            val domain = dnsMessage.questionName
            
            val status = FilterEngine.checkDomain(domain)
            
            var (dnsResponse, statusString) = when {
                FilterEngine.isDohBypass(domain) -> {
                    // Treat DoH bypass as BLOCKED for now, or create new status
                    VpnStats.increment(vpnService, domain, FilterEngine.FilterStatus.BLOCKED, appName) 
                    Log.i("DnsProxy", "BLOCKING (DoH Bypass): $domain")
                    Pair(dnsMessage.createErrorResponse(), "BLOCKED_DOH")
                }
                status == FilterEngine.FilterStatus.BLOCKED -> {
                    VpnStats.increment(vpnService, domain, FilterEngine.FilterStatus.BLOCKED, appName)
                    Log.i("DnsProxy", "BLOCKED: $domain from $appName")
                    Pair(dnsMessage.createBlockedResponse(), "BLOCKED")
                }
                else -> {
                    // ALLOWED_USER, ALLOWED_SYSTEM, or ALLOWED_DEFAULT
                    VpnStats.increment(vpnService, domain, status, appName)
                    Log.i("DnsProxy", "ALLOWING ($status): $domain from $appName")
                    
                    var response: ByteArray? = null
                    
                    // 1. Attempt DNS-over-HTTPS (if enabled)
                    if (useDoh) {
                        response = DohClient.resolve(dnsQuery)
                    }
                    
                    // 2. Fallback or use standard UDP Pooling
                    if (response == null) {
                        response = resolveViaUdp(dnsQuery, domain, dnsMessage.transactionId)
                    }
                    
                    Pair(response, if (response != null) "RESOLVED" else "FAILED")
                }
            }

            if (dnsResponse == null) {
                Log.w("DnsProxy", "Resolution failed for $domain")
                dnsResponse = dnsMessage.createErrorResponse()
            }

            requestPacket.position(0)
            return PacketUtils.createUdpResponse(requestPacket, dnsResponse)
        } catch (e: Exception) {
            Log.e("DnsProxy", "Critical DNS Proxy fail", e)
            return null
        }
    }
}
