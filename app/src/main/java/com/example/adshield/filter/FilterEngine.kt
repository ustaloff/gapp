package com.example.adshield.filter

object FilterEngine {

    // Using a simple in-memory set for the blocklist.
    // In a real app, this would be loaded from a file or database.
    // Using a volatile variable for thread-safe replacement of the read-only set.
    @Volatile
    private var blocklist: Set<String> = setOf(
        // === FALLBACK / BOOTSTRAP LIST ===
        // Used before external lists are downloaded
        // ... (Keep the critical ones) ...
        "doubleclick.net",
        "ad.google.com",
        "mc.yandex.ru",
        "an.yandex.ru",
        "videoroll.net",
        "marketgid.com",
        "mgid.com",
        "googleadservices.com"
        // REMOVED gvt1/gvt2 as they break video streaming
    )
    
    // Safety Allowlist - These domains are NEVER blocked
    private val allowlist = setOf(
        "facebook.com",
        "www.facebook.com",
        "twitch.tv",
        "www.twitch.tv",
        "ttvnw.net", // Twitch video CDN
        "youtube.com",
        "www.youtube.com",
        "googlevideo.com",
        "hdrezka.ac",
        "hdrezka.ag",
        "rezka.ag",
        "static.hdrezka.ac",
        "hls.hdrezka.ac",
        // Additional critical infrastructures
        "google.com",
        "googleapis.com",
        "gstatic.com",
        "amazon.com",
        "amazonaws.com",
        "cloudfront.net",
        "cdn.net",
        "cloudflare.com",
        "microsoft.com",
        "apple.com"
    )

    fun updateBlocklist(newRules: Set<String>) {
        if (newRules.isNotEmpty()) {
            // Merge with fallback or just replace? 
            // Replacing is cleaner for "update", but merging with a core "safety net" is often better.
            // Let's replace but ensure our hardcoded favorites are included if we want strictness.
            // For now, simpler is better: replacement.
            blocklist = newRules
            android.util.Log.i("FilterEngine", "Blocklist updated. New size: ${blocklist.size}")
        }
    }

    fun getRuleCount(): Int = blocklist.size

    /**
     * Checks if a domain should be blocked.
     * This implementation checks if the domain itself or any of its parent domains
     * are in the blocklist.
     *
     * Example: if "doubleclick.net" is in the list, "ad.doubleclick.net" will be blocked.
     */
    fun shouldBlock(domain: String?): Boolean {
        if (domain.isNullOrBlank()) {
            return false
        }

        var currentDomain = domain.lowercase()
        
        // 0. Safety Check: Allowlist
        // If the domain or its parent is in the allowlist, NEVER block it.
        var tempDomain = currentDomain
        while (tempDomain.isNotEmpty()) {
             if (allowlist.contains(tempDomain)) {
                 // android.util.Log.d("FilterEngine", "ALLOWLISTED: $currentDomain (matched $tempDomain)")
                 return false
             }
             val dotIndex = tempDomain.indexOf('.')
             if (dotIndex == -1) break
             tempDomain = tempDomain.substring(dotIndex + 1)
        }
        
        // Save original for logging
        val originalDomain = currentDomain

        while (currentDomain.isNotEmpty()) {
            if (blocklist.contains(currentDomain)) {
                android.util.Log.i("FilterEngine", "BLOCKED: $originalDomain (matched $currentDomain)")
                return true
            }
            val dotIndex = currentDomain.indexOf('.')
            if (dotIndex == -1) {
                break
            }
            currentDomain = currentDomain.substring(dotIndex + 1)
        }
        
        // Debug Log: What are we letting through?
        // android.util.Log.d("FilterEngine", "ALLOWED: $originalDomain")
        return false
    }
}
