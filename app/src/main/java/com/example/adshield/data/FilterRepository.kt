package com.example.adshield.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object FilterRepository {

    data class FilterData(
        val blockRules: Set<String>,
        val exceptionRules: Set<String>
    )

    private const val TAG = "FilterRepository"

    // Default fallback (Custom AdShield Blocklist)
    private const val DEFAULT_URL =
        "https://raw.githubusercontent.com/ustaloff/adshield-lists/refs/heads/master/blocklist.txt"

    suspend fun downloadAndParseFilters(context: android.content.Context): FilterData =
        withContext(Dispatchers.IO) {
            val blockRules = mutableSetOf<String>()
            val exceptionRules = mutableSetOf<String>()
            val start = System.currentTimeMillis()

            // Load URL from Preferences
            val prefs = AppPreferences(context)
            val targetUrl = prefs.getFilterSourceUrl()

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
            return@withContext FilterData(blockRules, exceptionRules)
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
            trimmed = trimmed.substring(0, separatorIndex)
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
