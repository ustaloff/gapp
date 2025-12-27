package com.example.adshield.net

import java.nio.ByteBuffer

class DnsMessage(private val rawData: ByteArray) {

    val questionName: String
    private val questionSection: ByteArray
    val transactionId: Short

    private val qType: Int
    private val qClass: Int

    init {
        val buffer = ByteBuffer.wrap(rawData)
        transactionId = buffer.short
        buffer.position(12) // Skip to the start of the questions

        val nameData = parseName(buffer)
        questionName = nameData.first

        qType = buffer.short.toInt() and 0xFFFF
        qClass = buffer.short.toInt() and 0xFFFF

        val questionEndPos = buffer.position()
        questionSection = rawData.sliceArray(12 until questionEndPos)
    }

    private fun parseName(buffer: ByteBuffer, depth: Int = 0): Pair<String, ByteArray> {
        if (depth > 10) return Pair("", byteArrayOf(0)) // Prevent infinite recursion

        val nameParts = mutableListOf<String>()
        val nameBuffer = mutableListOf<Byte>()
        val startPos = buffer.position()

        while (buffer.hasRemaining()) {
            val length = buffer.get().toInt() and 0xFF
            if (length == 0) {
                nameBuffer.add(0.toByte())
                break // End of name
            }
            if ((length and 0xC0) == 0xC0) { // Pointer
                val pointer = (length and 0x3F) shl 8 or (buffer.get().toInt() and 0xFF)
                val currentPos = buffer.position()
                buffer.position(pointer)
                nameParts.add(parseName(buffer, depth + 1).first)
                buffer.position(currentPos)
                nameBuffer.add(length.toByte())
                nameBuffer.add((pointer and 0xFF).toByte())
                break
            } else {
                nameBuffer.add(length.toByte())
                val label = ByteArray(length)
                buffer.get(label)
                nameBuffer.addAll(label.toList())
                nameParts.add(String(label))
            }
        }

        val rawNameBytes = buffer.array().sliceArray(startPos until buffer.position())
        return Pair(nameParts.joinToString("."), rawNameBytes)
    }

    fun createBlockedResponse(): ByteArray {
        // Safe allocation: Header + Question + Answer (standard A record)
        val responseBuffer = ByteBuffer.allocate(rawData.size + 128)

        // Header
        responseBuffer.putShort(transactionId) // Transaction ID
        responseBuffer.putShort(0x8180.toShort()) // Flags: Standard response, no error
        responseBuffer.putShort(1) // QDCOUNT

        // Only provide an answer for A (1) or AAAA (28) records.
        // For others (HTTPS, SVCB, etc.), return 0 answers (Standard block)
        val hasAddressAnswer = qType == 1 || qType == 28
        responseBuffer.putShort(if (hasAddressAnswer) 1 else 0) // ANCOUNT
        responseBuffer.putShort(0)
        responseBuffer.putShort(0)

        // Question section (copy from original)
        responseBuffer.put(questionSection)

        if (hasAddressAnswer) {
            // Answer section
            responseBuffer.putShort(0xC00C.toShort()) // Pointer to name at offset 12
            responseBuffer.putShort(qType.toShort())  // Preserve original type (A or AAAA)
            responseBuffer.putShort(qClass.toShort()) // Preserve original class (IN)
            responseBuffer.putInt(60) // TTL

            if (qType == 28) { // AAAA
                responseBuffer.putShort(16) // RDLENGTH for IPv6
                responseBuffer.put(ByteArray(16)) // RDATA: ::
            } else { // TYPE A
                responseBuffer.putShort(4) // RDLENGTH for IPv4
                responseBuffer.put(byteArrayOf(0, 0, 0, 0)) // RDATA: 0.0.0.0
            }
        }

        val responseSize = responseBuffer.position()
        val finalResponse = ByteArray(responseSize)
        responseBuffer.rewind()
        responseBuffer.get(finalResponse)

        return finalResponse
    }

    fun createErrorResponse(): ByteArray {
        // Safe allocation: Header + Question
        val responseBuffer = ByteBuffer.allocate(rawData.size + 64)

        // Header
        responseBuffer.putShort(transactionId) // Transaction ID
        responseBuffer.putShort(0x8183.toShort()) // Flags: NXDOMAIN (RCODE 3) instead of SERVFAIL
        responseBuffer.putShort(1) // QDCOUNT
        responseBuffer.putShort(0)
        responseBuffer.putShort(0)
        responseBuffer.putShort(0)

        // Question section
        responseBuffer.put(questionSection)

        val responseSize = responseBuffer.position()
        val finalResponse = ByteArray(responseSize)
        responseBuffer.rewind()
        responseBuffer.get(finalResponse)

        return finalResponse
    }

    fun getQTypeName(): String {
        return when (qType) {
            1 -> "A"
            2 -> "NS"
            5 -> "CNAME"
            6 -> "SOA"
            12 -> "PTR"
            15 -> "MX"
            16 -> "TXT"
            28 -> "AAAA"
            33 -> "SRV"
            64 -> "SVCB"
            65 -> "HTTPS"
            255 -> "ANY"
            else -> "TYPE_$qType"
        }
    }
}
