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

        android.util.Log.i(
            "PacketUtils",
            "Creating $version response for payload size: ${payload.size}"
        )
        return when (version) {
            4 -> createUdpResponseV4(request, payload)
            6 -> createUdpResponseV6(request, payload)
            else -> {
                android.util.Log.e("PacketUtils", "Unknown IP version: $version")
                null
            }
        }
    }

    fun createUdpResponseV4(request: ByteBuffer, payload: ByteArray): ByteBuffer? {
        val requestIpHeaderLen = getIpHeaderLength(request)
        val responseIpHeaderLen = 20
        val responseTotalLen = responseIpHeaderLen + UDP_HEADER_SIZE + payload.size

        val response = ByteBuffer.allocate(responseTotalLen)
        response.put(IP_VERSION_AND_IHL)
        response.put(0.toByte()) // DSCP/ECN
        response.putShort(responseTotalLen.toShort())
        response.putShort(0) // Identification: 0 is fine for non-fragmented
        response.putShort(0x4000.toShort()) // Flags: Don't Fragment
        response.put(64) // TTL
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

        // UDP Header
        val udpHeaderAndPayload = ByteArray(UDP_HEADER_SIZE + payload.size)
        val udpBuffer = ByteBuffer.wrap(udpHeaderAndPayload)

        val srcPort = request.getShort(requestIpHeaderLen)
        val dstPort = request.getShort(requestIpHeaderLen + 2)

        udpBuffer.putShort(dstPort)
        udpBuffer.putShort(srcPort)
        udpBuffer.putShort((UDP_HEADER_SIZE + payload.size).toShort())
        udpBuffer.putShort(0)
        udpBuffer.put(payload)

        val udpChecksum = calculateUdpChecksumV4(dstIp, srcIp, udpHeaderAndPayload)
        ByteBuffer.wrap(udpHeaderAndPayload).putShort(6, udpChecksum)

        response.put(udpHeaderAndPayload)
        response.flip()
        return response
    }

    fun createUdpResponseV6(request: ByteBuffer, payload: ByteArray): ByteBuffer? {
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

        // Checksum placeholder
        val checksumPos = response.position()
        response.putShort(0)

        // Calculate and put actual checksum
        val checksum = calculateIPv6Checksum(response, dstIp, srcIp, payload)
        response.putShort(checksumPos, checksum)

        response.put(payload)
        response.flip()
        return response
    }


    private fun calculateIPv6Checksum(
        buffer: ByteBuffer,
        srcIp: ByteArray,
        dstIp: ByteArray,
        payload: ByteArray
    ): Short {
        var sum = 0L

        // 1. Pseudo-header: Source & Destination Address (16 words)
        for (i in 0 until 8) {
            sum += ((srcIp[i * 2].toInt() and 0xFF) shl 8 or (srcIp[i * 2 + 1].toInt() and 0xFF)).toLong()
            sum += ((dstIp[i * 2].toInt() and 0xFF) shl 8 or (dstIp[i * 2 + 1].toInt() and 0xFF)).toLong()
        }

        // 2. Pseudo-header: UDP Length (32-bit field in Pseudo-header)
        val udpLen = UDP_HEADER_SIZE + payload.size
        sum += (udpLen shr 16).toLong()
        sum += (udpLen and 0xFFFF).toLong()

        // 3. Pseudo-header: Next Header (8-bit field, padded with zeros)
        sum += 0L // Zeroes
        sum += PROTOCOL_UDP.toLong()

        // 4. UDP Header (excluding checksum)
        sum += (buffer.getShort(40).toInt() and 0xFFFF).toLong() // srcPort
        sum += (buffer.getShort(42).toInt() and 0xFFFF).toLong() // dstPort
        sum += udpLen.toLong() // Length again (from actual UDP header)

        // 5. Payload
        var i = 0
        while (i < payload.size - 1) {
            sum += ((payload[i].toInt() and 0xFF) shl 8 or (payload[i + 1].toInt() and 0xFF)).toLong()
            i += 2
        }
        if (i < payload.size) {
            sum += (payload[i].toInt() and 0xFF shl 8).toLong()
        }

        // 6. Final Fold to 16 bits
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        val final = (sum.inv() and 0xFFFF).toShort()
        // UDP checksum cannot be 0 in IPv6; if result is 0, use 0xFFFF
        return if (final == 0.toShort()) 0xFFFF.toShort() else final
    }

    private fun calculateChecksum(buffer: ByteBuffer, offset: Int, length: Int): Short {
        var sum = 0L
        var i = offset

        while (i < offset + length - 1) {
            val word =
                (buffer.get(i).toInt() and 0xFF shl 8) or (buffer.get(i + 1).toInt() and 0xFF)
            sum += word.toLong()
            i += 2
        }

        if (i < offset + length) {
            sum += (buffer.get(i).toInt() and 0xFF shl 8).toLong()
        }

        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return (sum.inv() and 0xFFFF).toShort()
    }

    // IPv4 UDP Checksum with Pseudo-header
    private fun calculateUdpChecksumV4(
        srcIp: ByteArray,
        dstIp: ByteArray,
        udpHeaderAndPayload: ByteArray
    ): Short {
        var sum = 0L

        // Pseudo-header
        for (i in 0 until 2) {
            sum += ((srcIp[i * 2].toInt() and 0xFF) shl 8 or (srcIp[i * 2 + 1].toInt() and 0xFF)).toLong()
            sum += ((dstIp[i * 2].toInt() and 0xFF) shl 8 or (dstIp[i * 2 + 1].toInt() and 0xFF)).toLong()
        }

        sum += PROTOCOL_UDP.toLong()
        sum += udpHeaderAndPayload.size.toLong()

        // UDP Header (excluding checksum field)
        // Ports and Length are in the first 6 bytes of udpHeaderAndPayload
        var i = 0
        while (i < udpHeaderAndPayload.size - 1) {
            if (i == 6) { // Skip Checksum field
                i += 2
                continue
            }
            val word =
                (udpHeaderAndPayload[i].toInt() and 0xFF shl 8) or (udpHeaderAndPayload[i + 1].toInt() and 0xFF)
            sum += word.toLong()
            i += 2
        }

        if (i < udpHeaderAndPayload.size) {
            sum += (udpHeaderAndPayload[i].toInt() and 0xFF shl 8).toLong()
        }

        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        val final = (sum.inv() and 0xFFFF).toShort()
        return if (final == 0.toShort()) 0xFFFF.toShort() else final
    }
}
