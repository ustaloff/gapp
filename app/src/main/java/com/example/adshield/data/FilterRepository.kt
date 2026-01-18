package com.example.adshield.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withContext
import java.net.URL
import com.example.adshield.data.AppConfig // Ensure AppConfig is imported if not already, though used in previous step
import com.example.adshield.data.BillingManager
import com.example.adshield.data.UserAccessState

object FilterRepository {

    data class FilterData(
        val blockRules: Set<String>,
        val exceptionRules: Set<String>
    )

    private const val TAG = "FilterRepository"

    // Default fallback (Custom AdShield Blocklist)
    // moved to AppConfig.DEFAULT_FILTER_URL

    suspend fun downloadAndParseFilters(context: android.content.Context): FilterData =
        withContext(Dispatchers.IO) {
            val blockRules = mutableSetOf<String>()
            val exceptionRules = mutableSetOf<String>()
            val start = System.currentTimeMillis()

            // Load URL from Preferences
            val prefs = AppPreferences(context)
            // ENFORCEMENT: If User is FREE, always use Default URL (Paywall Logic)
            val userAccess = BillingManager.currentUserAccess.value
            val targetUrl = if (userAccess.state == UserAccessState.FREE) {
                Log.i(TAG, "User is FREE. Enforcing Default Filter URL.")
                AppConfig.DEFAULT_FILTER_URL
            } else {
                prefs.getFilterSourceUrl()
            }

            Log.i(TAG, "Starting filter download from: $targetUrl")

            try {
                val url = URL(targetUrl)
                val content = url.readText()

                Log.i(TAG, "Download complete. Size: ${content.length} chars. Parsing...")

                content.lineSequence().forEach { line ->
                    val result = parseLine(line)
                    if (result != null) {
                        val (rule, isException) = result
                        if (isException) {
                            exceptionRules.add(rule)
                        } else {
                            blockRules.add(rule)
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Failed to download filters", e)
            }

            Log.i(
                TAG,
                "Loaded ${blockRules.size} block rules and ${exceptionRules.size} exception rules in ${System.currentTimeMillis() - start}ms"
            )

            // Record success timestamp
            prefs.setLastFilterUpdate(System.currentTimeMillis())

            return@withContext FilterData(blockRules, exceptionRules)
        }

    /**
     * Verifies a custom filter URL by attempting to download and parse it.
     * Enforces strict safety checks:
     * 1. Must be HTTP/HTTPS (no file://).
     * 2. Must be < 5MB (prevent OOM).
     * 3. Must contain valid rules.
     *
     * @return Result<Int> containing the count of valid rules found, or an exception.
     */
    suspend fun verifyUrl(urlString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Protocol Check
            if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                return@withContext Result.failure(IllegalArgumentException("Only http/https URLs are supported"))
            }

            // 2. Download and Size Check
            val url = URL(urlString)
            val connection = url.openConnection()
            connection.connectTimeout = AppConfig.FILTER_DOWNLOAD_TIMEOUT_MS
            connection.readTimeout = AppConfig.FILTER_DOWNLOAD_TIMEOUT_MS
            // Note: contentLength might be -1 if unknown, so we also limit the read loop
            val length = connection.contentLength
            if (length > AppConfig.FILTER_MAX_SIZE_BYTES) { // 5MB Limit
                return@withContext Result.failure(IllegalArgumentException("File too large (Max 5MB)"))
            }

            val stream = url.openStream()
            val savedContent = StringBuilder()
            val buffer = ByteArray(8 * 1024)
            var bytesRead: Int
            var totalRead = 0

            stream.use { input ->
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    totalRead += bytesRead
                    if (totalRead > AppConfig.FILTER_MAX_SIZE_BYTES) {
                        return@withContext Result.failure(IllegalArgumentException("File too large (Max 5MB)"))
                    }
                    savedContent.append(String(buffer, 0, bytesRead))
                }
            }

            // 3. Parse and Count
            var validRules = 0
            savedContent.toString().lineSequence().forEach { line ->
                if (parseLine(line) != null) validRules++
            }

            if (validRules == 0) {
                return@withContext Result.failure(Exception("No valid rules found in file"))
            }

            return@withContext Result.success(validRules)

        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }

    /**
     * Extracts a domain from an AdBlock/EasyList rule.
     * Supports:
     * - ||example.com^ (Domain block)
     * - 0.0.0.0 example.com (Hosts format)
     * - example.com (Simple hosts format)
     *
     * Ignores:
     * - ! Comments
     * - [Adblock Plus 2.0] headers
     * - cosmetic rules (##, #@#)
     */

    @Suppress("SpellCheckingInspection")
    private fun parseLine(line: String): Pair<String, Boolean>? {
        var trimmed = line.trim()

        // 1. Identify Exception Rules (@@)
        val isException = trimmed.startsWith("@@")
        if (isException) {
            trimmed = trimmed.substring(2)
        }

        // 2. Ignore Comments (!), Metadata ([), Cosmetic rules (#), and Empty lines
        if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[") || trimmed.contains(
                "#"
            )
        ) {
            return null
        }

        // 3. Strip AdBlock Options ($)
        // DNS filtering cannot handle specific resource types ($script, $image, etc.)
        // We strip them and keep the core domain rule.
        if (trimmed.contains("$")) {
            val parts = trimmed.split("$")
            val options = parts.getOrNull(1) ?: ""

            // For exceptions, if it's too specific and NOT a site-wide whitelist, skip it
            if (isException) {
                val hasGenericException =
                    options.contains("document") || options.split(",").all { it.isEmpty() }
                val hasSpecificRestriction =
                    options.contains("script") || options.contains("image") ||
                            options.contains("subdocument") || options.contains("xmlhttprequest") ||
                            options.contains("domain")

                if (hasSpecificRestriction && !hasGenericException) {
                    return null // Too specific for DNS
                }
            }
            trimmed = parts[0]
        }

        // 4. Handle ||domain.com^ (Standard AdBlock blocking rule)
        // We strip || and ^ to turn it into a clean domain rule for the Trie
        if (trimmed.startsWith("||")) {
            trimmed = trimmed.substring(2)
        } else {
            // DNS SAFETY: If a rule doesn't start with || and contains a '/', 
            // it's likely a path-specific rule (e.g. example.com/ads/)
            // We MUST NOT block the whole domain for such rules at the DNS level.
            if (trimmed.contains("/")) return null
        }

        // Remove trailing ^ or /
        val separatorIndex = trimmed.indexOfAny(charArrayOf('^', '/'))
        if (separatorIndex != -1) {
            trimmed = trimmed.take(separatorIndex)
        }

        trimmed = trimmed.lowercase().trim()
        if (trimmed.isEmpty() || trimmed.length < 3) return null

        // 5. Handle Hosts format (0.0.0.0 example.com)
        if (trimmed.startsWith("0.0.0.0 ") || trimmed.startsWith("127.0.0.1 ")) {
            val parts = trimmed.split(" ").filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val domain = parts[1].trim()
                if (domain.contains('.') && domain != "localhost") {
                    return Pair(domain, isException)
                }
            }
        }

        // 6. Simple Domain list (adserver.com or cleaned AdBlock rule)
        if (trimmed.contains(".")) {
            return Pair(trimmed, isException)
        }

        return null
    }
}
