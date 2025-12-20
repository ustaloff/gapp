package com.example.adshield.filter

object FilterEngine {

    // Using a simple in-memory set for the blocklist.
    // In a real app, this would be loaded from a file or database.
    private val blocklist = setOf(
        // Common ad/tracker domains
        "doubleclick.net",
        "ad.google.com",
        "googleadservices.com",
        "googlesyndication.com",
        "admob.google.com",
        "pagead2.googlesyndication.com",
        "graph.facebook.com",
        "connect.facebook.net",
        "app-measurement.com",
        "crashlytics.com",
        "analytics.google.com",

        // Example test domains
        "block.me",
        "ads.example.com"
    )

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

        var currentDomain = domain.toLowerCase()

        while (currentDomain.isNotEmpty()) {
            if (blocklist.contains(currentDomain)) {
                return true
            }
            val dotIndex = currentDomain.indexOf('.')
            if (dotIndex == -1) {
                break
            }
            currentDomain = currentDomain.substring(dotIndex + 1)
        }

        return false
    }
}
