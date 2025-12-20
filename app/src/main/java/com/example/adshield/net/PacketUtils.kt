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
        val firstByte = packet.get(0).toInt()
        val version = (firstByte shr 4) and 0x0F
        return if (version == 4) {
            (firstByte and 0x0F) * 4
        } else if (version == 6) {
            40
        } else {
            0
        }
    }

    fun createUdpResponse(request: ByteBuffer, payload: ByteArray): ByteBuffer? {
        val firstByte = request.get(0).toInt()
        val version = (firstByte shr 4) and 0x0F

        return if (version == 4) {
            createUdpResponseV4(request, payload)
        } else if (version == 6) {
            createUdpResponseV6(request, payload)
        } else {
            null
        }
    }

    private fun createUdpResponseV4(request: ByteBuffer, payload: ByteArray): ByteBuffer? {
        val requestIpHeaderLen = getIpHeaderLength(request)
        val responseIpHeaderLen = 20
        val responseTotalLen = responseIpHeaderLen + UDP_HEADER_SIZE + payload.size
        
        val response = ByteBuffer.allocate(responseTotalLen)
        response.put(IP_VERSION_AND_IHL)
        response.put(request.get(1))
        response.putShort(responseTotalLen.toShort())
        response.putShort(request.getShort(4))
        response.putShort(request.getShort(6))
        response.put(64)
        response.put(PROTOCOL_UDP)
        response.putShort(0) // Checksum placeholder

        // Swap addresses
        val srcIp = ByteArray(4)
        val dstIp = ByteArray(4)
        request.position(12); request.get(srcIp)
        request.position(16); request.get(dstIp)
        response.put(dstIp)
        response.put(srcIp)

        response.putShort(10, calculateChecksum(response, 0, responseIpHeaderLen))

        // UDP
        val srcPort = request.getShort(requestIpHeaderLen)
        val dstPort = request.getShort(requestIpHeaderLen + 2)
        response.putShort(dstPort)
        response.putShort(srcPort)
        response.putShort((UDP_HEADER_SIZE + payload.size).toShort())
        response.putShort(0)
        
        response.put(payload)
        response.flip()
        return response
    }

    private fun createUdpResponseV6(request: ByteBuffer, payload: ByteArray): ByteBuffer? {
        val responseIpHeaderLen = 40
        val responseTotalLen = responseIpHeaderLen + UDP_HEADER_SIZE + payload.size
        
        val response = ByteBuffer.allocate(responseTotalLen)
        
        // IPv6 Header
        // Version 6, Traffic Class 0, Flow Label 0
        response.putInt(0x60000000.toInt())
        response.putShort((UDP_HEADER_SIZE + payload.size).toShort()) // Payload length
        response.put(17) // Next Header: UDP
        response.put(64) // Hop Limit

        // Swap IPv6 Addresses (16 bytes each)
        val srcIp = ByteArray(16)
        val dstIp = ByteArray(16)
        request.position(8); request.get(srcIp)
        request.position(24); request.get(dstIp)
        response.put(dstIp)
        response.put(srcIp)

        // UDP Header
        val srcPort = request.getShort(40)
        val dstPort = request.getShort(42)
        response.putShort(dstPort)
        response.putShort(srcPort)
        response.putShort((UDP_HEADER_SIZE + payload.size).toShort())
        response.putShort(0) // UDP checksum - complicated in IPv6, 0 often works for local TUN

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
