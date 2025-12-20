package com.example.adshield.net

import java.nio.ByteBuffer

class DnsMessage(private val rawData: ByteArray) {

    val questionName: String
    private val questionSection: ByteArray
    private val transactionId: Short

    init {
        val buffer = ByteBuffer.wrap(rawData)
        transactionId = buffer.short
        buffer.position(12) // Skip to the start of the questions

        val nameData = parseName(buffer)
        questionName = nameData.first

        val questionEndPos = buffer.position() + 4 // after name, there is QTYPE and QCLASS (2+2=4 bytes)
        questionSection = rawData.sliceArray(12 until questionEndPos)
    }

    private fun parseName(buffer: ByteBuffer): Pair<String, ByteArray> {
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
                nameParts.add(parseName(buffer).first) // Recursively parse pointed-to name
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
        val responseBuffer = ByteBuffer.allocate(512)

        // Header
        responseBuffer.putShort(transactionId) // Transaction ID
        responseBuffer.putShort(0x8180.toShort()) // Flags: Standard response, no error
        responseBuffer.putShort(1) // QDCOUNT (Question Count)
        responseBuffer.putShort(1) // ANCOUNT (Answer Count)
        responseBuffer.putShort(0) // NSCOUNT
        responseBuffer.putShort(0) // ARCOUNT

        // Question section (copy from original)
        responseBuffer.put(questionSection)

        // Answer section
        responseBuffer.putShort(0xC00C.toShort()) // Name pointer to offset 12 (the start of the name in the Question)
        responseBuffer.putShort(1) // TYPE: A (Host Address)
        responseBuffer.putShort(1) // CLASS: IN (Internet)
        responseBuffer.putInt(60) // TTL: 60 seconds
        responseBuffer.putShort(4) // RDLENGTH: 4 bytes for IPv4
        responseBuffer.put(byteArrayOf(0, 0, 0, 0)) // RDATA: 0.0.0.0

        val responseSize = responseBuffer.position()
        val finalResponse = ByteArray(responseSize)
        responseBuffer.rewind()
        responseBuffer.get(finalResponse)

        return finalResponse
    }
}
