package com.example.adshield.net

import android.net.VpnService
import android.util.Log
import com.example.adshield.data.VpnStats
import com.example.adshield.filter.FilterEngine
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class DnsProxy(private val vpnService: VpnService) {

    private val upstreamDns = "8.8.8.8"
    private val upstreamPort = 53

    fun handleDnsRequest(requestPacket: ByteBuffer): ByteBuffer? {
        try {
            // The requestPacket is the entire IP packet. We only need the DNS payload.
            val ipHeaderSize = PacketUtils.getIpHeaderLength(requestPacket)
            val payloadOffset = ipHeaderSize + PacketUtils.UDP_HEADER_SIZE
            val payloadSize = requestPacket.limit() - payloadOffset
            if (payloadSize <= 0) return null

            val dnsQuery = ByteArray(payloadSize)
            requestPacket.position(payloadOffset)
            requestPacket.get(dnsQuery)

            val dnsMessage = DnsMessage(dnsQuery)
            val domain = dnsMessage.questionName

            Log.d("DnsProxy", "Query for: $domain")

            val dnsResponse: ByteArray
            if (FilterEngine.shouldBlock(domain)) {
                Log.i("DnsProxy", "[BLOCKED] $domain")
                VpnStats.incrementBlocked()
                dnsResponse = dnsMessage.createBlockedResponse()
            } else {
                VpnStats.incrementTotal()
                // Forward to a real DNS server
                val socket = DatagramSocket()
                vpnService.protect(socket) // IMPORTANT: Exclude this socket from the VPN

                val upstreamAddr = InetAddress.getByName(upstreamDns)
                val outPacket = DatagramPacket(dnsQuery, dnsQuery.size, upstreamAddr, upstreamPort)
                socket.send(outPacket)

                val responseBuffer = ByteArray(1500)
                val inPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                socket.soTimeout = 2000 // 2-second timeout
                socket.receive(inPacket)
                socket.close()

                dnsResponse = inPacket.data.sliceArray(0 until inPacket.length)
            }

            // Wrap the DNS response in a new IP/UDP packet to send back to the app
            return PacketUtils.createUdpResponse(requestPacket, dnsResponse)

        } catch (e: Exception) {
            Log.e("DnsProxy", "Failed to handle DNS packet", e)
            return null
        }
    }
}
