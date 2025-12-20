package com.example.adshield.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

object FilterRepository {

    private const val TAG = "FilterRepository"
    
    // EasyList + RuAdList (Combined) - Official stable mirror
    private const val RU_ADLIST_URL = "https://easylist-downloads.adblockplus.org/ruadlist+easylist.txt"
    private const val EASYLIST_URL = "https://easylist.to/easylist/easylist.txt"

    suspend fun downloadAndParseFilters(): Set<String> = withContext(Dispatchers.IO) {
        val rules = mutableSetOf<String>()
        val start = System.currentTimeMillis()
        
        Log.i(TAG, "Starting filter download...")

        try {
            // We can download multiple lists. For now, let's start with RuAdList/EasyList
            // Note: This might be large (megabytes).
            val url = URL(RU_ADLIST_URL)
            val content = url.readText()
            
            Log.i(TAG, "Download complete. Size: ${content.length} chars. Parsing...")
            
            content.lineSequence().forEach { line ->
                val domain = parseLine(line)
                if (domain != null) {
                    rules.add(domain)
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download filters", e)
             // Return empty set on failure so we don't clear existing hardcoded rules if we rely on them?
             // Or maybe we returns what we have.
        }

        Log.i(TAG, "Loaded ${rules.size} rules in ${System.currentTimeMillis() - start}ms")
        return@withContext rules
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
    private fun parseLine(line: String): String? {
        val trimmed = line.trim()
        
        // 1. Ignore Comments (!), Metadata ([), and Exception Rules (@@)
        if (trimmed.isEmpty() || trimmed.startsWith("!") || trimmed.startsWith("[") || trimmed.startsWith("@@")) {
            return null
        }

        // 2. Handle ||domain.com^ (Standard AdBlock blocking rule)
        if (trimmed.startsWith("||")) {
            // Extract content between || and ^ or /
            // Example: ||ads.facebook.com^ -> ads.facebook.com
            // Example: ||google-analytics.com/ -> google-analytics.com
            
            var domain = trimmed.substring(2)
            
            val separatorIndex = domain.indexOfAny(charArrayOf('^', '/'))
            if (separatorIndex != -1) {
                domain = domain.substring(0, separatorIndex)
            }
            
            domain = domain.lowercase()

            // Validate: Must look like a domain (have a dot, no weird chars)
            if (domain.contains('.') && !domain.contains('*') && !domain.contains('=')) {
                return domain
            }
        }

        // 3. Handle Hosts format (0.0.0.0 example.com)
        if (trimmed.startsWith("0.0.0.0 ") || trimmed.startsWith("127.0.0.1 ")) {
            val parts = trimmed.split(" ")
            if (parts.size >= 2) {
                val domain = parts[1].trim().lowercase()
                 if (domain.contains('.') && domain != "localhost") {
                     return domain
                 }
            }
        }

        // 4. Ignore Cosmetic Rules (element hiding)
        // (Rules with ## or #@# are cosmetic CSS selectors, not DNS blocks)
        
        return null
    }
}
