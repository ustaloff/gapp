package com.example.adshield.net

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.net.HttpURLConnection
import java.net.URL

object DohClient {
    private const val TAG = "DohClient"
    private const val PRIMARY_DOH = "https://cloudflare-dns.com/dns-query"
    private const val BACKUP_DOH = "https://dns.google/dns-query"

    suspend fun resolve(dnsQuery: ByteArray): ByteArray? = withContext(Dispatchers.IO) {
        // Try Cloudflare first, then Google as fallback
        return@withContext tryDoh(PRIMARY_DOH, dnsQuery) ?: tryDoh(BACKUP_DOH, dnsQuery)
    }

    private fun tryDoh(providerUrl: String, dnsQuery: ByteArray): ByteArray? {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(providerUrl)
            connection = url.openConnection() as HttpURLConnection

            connection.apply {
                requestMethod = "POST"
                doOutput = true
                doInput = true
                connectTimeout = 2000
                readTimeout = 2000
                setRequestProperty("Content-Type", "application/dns-message")
                setRequestProperty("Accept", "application/dns-message")
            }

            BufferedOutputStream(connection.outputStream).use { os ->
                os.write(dnsQuery)
                os.flush()
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                BufferedInputStream(connection.inputStream).use { bis ->
                    return bis.readBytes()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "DoH failed for $providerUrl: ${e.message}")
        } finally {
            connection?.disconnect()
        }
        return null
    }
}
