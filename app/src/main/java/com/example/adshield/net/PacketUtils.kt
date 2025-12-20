package com.example.adshield.net

import java.nio.ByteBuffer

object PacketUtils {

    private const val IP_VERSION_4: Byte = 0x40
    private const val IP_IHL_5: Byte = 0x05 // 5 * 4 = 20 bytes
    private const val IP_VERSION_AND_IHL = (IP_VERSION_4 + IP_IHL_5).toByte()
    private const val PROTOCOL_UDP: Byte = 17
    const val UDP_HEADER_SIZE = 8

    fun getIpHeaderLength(packet: ByteBuffer): Int {
        if (packet.remaining() < 1) return 0
        // The IHL is the lower nibble of the first byte, value is number of 32-bit words.
        return (packet.get(0).toInt() and 0x0F) * 4
    }

    fun createUdpResponse(request: ByteBuffer, payload: ByteArray): ByteBuffer? {
        val requestIpHeaderLen = getIpHeaderLength(request)
        if (requestIpHeaderLen < 20) return null

        // Our response will have a standard 20-byte IP header, no options.
        val responseIpHeaderLen = 20
        val responseTotalLen = responseIpHeaderLen + UDP_HEADER_SIZE + payload.size
        if (responseTotalLen > 32767) return null // Packet too large

        val response = ByteBuffer.allocate(responseTotalLen)

        // --- IP Header ---
        response.put(IP_VERSION_AND_IHL)
        response.put(request.get(1)) // Type of Service (can be copied)
        response.putShort(responseTotalLen.toShort()) // Total Length
        response.putShort(request.getShort(4)) // Identification (can be copied)
        response.putShort(request.getShort(6)) // Flags & Fragment Offset (can be copied)
        response.put(64) // TTL (Time-to-Live)
        response.put(PROTOCOL_UDP) // Protocol (UDP)
        response.putShort(0) // Header Checksum (placeholder)

        // Swap IP addresses
        val newSourceIp = ByteArray(4)
        request.position(16) // Old destination IP
        request.get(newSourceIp)

        val newDestIp = ByteArray(4)
        request.position(12) // Old source IP
        request.get(newDestIp)

        response.put(newSourceIp)
        response.put(newDestIp)

        // Calculate and write IP checksum
        val ipChecksum = calculateChecksum(response, 0, responseIpHeaderLen)
        response.putShort(10, ipChecksum)

        // --- UDP Header ---
        val requestUdpOffset = requestIpHeaderLen
        val sourcePort = request.getShort(requestUdpOffset)
        val destPort = request.getShort(requestUdpOffset + 2)

        response.putShort(destPort) // New source port = old dest port
        response.putShort(sourcePort) // New dest port = old source port

        val udpLen = (UDP_HEADER_SIZE + payload.size).toShort()
        response.putShort(udpLen)
        response.putShort(0) // UDP checksum is optional in IPv4, 0 is fine

        // --- Payload ---
        response.put(payload)

        response.flip()
        return response
    }

    private fun calculateChecksum(buffer: ByteBuffer, offset: Int, length: Int): Short {
        var sum = 0
        var i = offset
        
        // Sum 16-bit words
        while (i < offset + length - 1) {
            // Get 16-bit word (Big Endian)
            val firstByte = buffer.get(i).toInt() and 0xFF
            val secondByte = buffer.get(i + 1).toInt() and 0xFF
            val word = (firstByte shl 8) or secondByte
            sum += word
            i += 2
        }

        // Handle odd byte
        if (i < offset + length) {
            val firstByte = buffer.get(i).toInt() and 0xFF
            sum += (firstByte shl 8)
        }

        // Fold 32-bit sum to 16 bits
        while ((sum shr 16) > 0) {
             sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return (sum.inv() and 0xFFFF).toShort()
    }
}
