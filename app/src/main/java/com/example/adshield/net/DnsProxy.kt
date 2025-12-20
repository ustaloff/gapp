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

    private val upstreamDns = "8.8.8.8"
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

            var dnsResponse: ByteArray? = null
            
            if (FilterEngine.shouldBlock(domain)) {
                VpnStats.incrementBlocked(domain, appName)
                dnsResponse = dnsMessage.createBlockedResponse()
            } else {
                VpnStats.incrementTotal(domain, appName)
                
                // 1. Attempt DNS-over-HTTPS (if enabled)
                if (useDoh) {
                    dnsResponse = DohClient.resolve(dnsQuery)
                }
                
                // 2. Fallback or use standard UDP Pooling
                if (dnsResponse == null) {
                    val socket = getPooledSocket()
                    try {
                        val upstreamAddr = InetAddress.getByName(upstreamDns)
                        val outPacket = DatagramPacket(dnsQuery, dnsQuery.size, upstreamAddr, upstreamPort)
                        
                        // Non-blocking approach within coroutine
                        withContext(Dispatchers.IO) {
                            socket.send(outPacket)
                            socket.soTimeout = 2000
                            
                            val responseBuffer = ByteArray(1500)
                            val inPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                            socket.receive(inPacket)
                            
                            if (inPacket.length > 0) {
                                dnsResponse = inPacket.data.sliceArray(0 until inPacket.length)
                            } else {
                                throw IOException("Empty DNS response")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("DnsProxy", "Upstream error for $domain: ${e.message}")
                        dnsResponse = dnsMessage.createErrorResponse() 
                    } finally {
                        returnToPool(socket)
                    }
                }
            }

            if (dnsResponse == null) return null

            requestPacket.position(0)
            return PacketUtils.createUdpResponse(requestPacket, dnsResponse)

        } catch (e: Exception) {
            Log.e("DnsProxy", "Critical DNS Proxy fail", e)
            return null
        }
    }
}
