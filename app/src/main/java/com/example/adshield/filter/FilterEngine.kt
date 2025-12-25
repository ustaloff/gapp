package com.example.adshield.filter

object FilterEngine {

    private class TrieNode {
        val children = mutableMapOf<String, TrieNode>()
        var isEndOfRule = false
    }

    private var root = TrieNode()
    private var exceptionRoot = TrieNode()
    // Stores the original rule string for each end-of-rule node for logging
    private val ruleMap = mutableMapOf<TrieNode, String>()
    private var ruleCount = 0
    private var exceptionCount = 0
    private val regexRules = mutableListOf<Regex>()
    private val exceptionRegexRules = mutableListOf<Regex>()
    private val userAllowlist = mutableSetOf<String>()
    private val userBlocklist = mutableSetOf<String>()
    
    // Safety Allowlist - Critical domains that should NEVER be blocked
    private val allowlist = mutableSetOf(
        "google.com", "googleapis.com", "gstatic.com", "googleusercontent.com", "ads.google.com",
        "facebook.com", "www.facebook.com", "fbcdn.net",
        "twitch.tv", "www.twitch.tv", "ttvnw.net",
        "youtube.com", "www.youtube.com", "googlevideo.com", "ytimg.com",
        "hdrezka.ac", "hdrezka.ag", "hdrezka.me", "hdrezka.co", "hdrezka.re", "hdrezka-home.tv",
        "rezka.ag", "rezka.me", "static.hdrezka.ac", "hls.hdrezka.ac",
        "voidboost.net", "voidboost.cc",
        "amazon.com", "amazonaws.com", "cloudfront.net", "cdn.net", "cloudflare.com", "microsoft.com", "apple.com", "icloud.com",
        "gvt1.com", "gvt2.com", "gvt3.com",
        "ss.lv", "m.ss.lv", "www.ss.lv", "inbox.lv"
    )
    
    // Domains used by browsers for DNS-over-HTTPS. Blocking these forces fallback to our VPN DNS.
    private val dohBypassList = setOf(
        "cloudflare-dns.com", "dns.google", "dns.nextdns.io", "doh.opendns.com", 
        "dns.quad9.net", "doh.cleanbrowsing.org"
    )
    
    fun isDohBypass(domain: String): Boolean {
        val clean = domain.lowercase().trim().trimEnd('.')
        return dohBypassList.any { clean == it || clean.endsWith(".$it") }
    }

    fun initialize(context: android.content.Context) {
        val prefs = com.example.adshield.data.AppPreferences(context)
        userAllowlist.clear()
        userAllowlist.addAll(prefs.getUserAllowlist())
        userBlocklist.clear()
        userBlocklist.addAll(prefs.getUserBlocklist())
    }

    fun addToAllowlist(context: android.content.Context, domain: String) {
        val cleanDomain = domain.trim().lowercase()
        if (cleanDomain.isNotEmpty()) {
            // Remove from blocklist if present to avoid conflicts
            removeFromBlocklist(context, cleanDomain)
            userAllowlist.add(cleanDomain)
            com.example.adshield.data.AppPreferences(context).addToUserAllowlist(cleanDomain)
        }
    }

    fun removeFromAllowlist(context: android.content.Context, domain: String) {
        val cleanDomain = domain.trim().lowercase()
        if (cleanDomain.isNotEmpty()) {
            userAllowlist.remove(cleanDomain)
            com.example.adshield.data.AppPreferences(context).removeFromUserAllowlist(cleanDomain)
        }
    }

    fun addToBlocklist(context: android.content.Context, domain: String) {
        val cleanDomain = domain.trim().lowercase()
        if (cleanDomain.isNotEmpty()) {
            // Remove from allowlist if present
            removeFromAllowlist(context, cleanDomain)
            userBlocklist.add(cleanDomain)
            com.example.adshield.data.AppPreferences(context).addToUserBlocklist(cleanDomain)
        }
    }

    fun removeFromBlocklist(context: android.content.Context, domain: String) {
        val cleanDomain = domain.trim().lowercase()
        if (cleanDomain.isNotEmpty()) {
            userBlocklist.remove(cleanDomain)
            com.example.adshield.data.AppPreferences(context).removeFromUserBlocklist(cleanDomain)
        }
    }

    init {
        // Load fallback rules immediately on startup
        updateBlocklist(com.example.adshield.data.FilterRepository.FilterData(emptySet(), emptySet()))
    }

    @Synchronized
    fun updateBlocklist(filterData: com.example.adshield.data.FilterRepository.FilterData) {
        // 1. Reset and Merge Fallback Rules
        ruleMap.clear()
        val finalBlockRules = filterData.blockRules.toMutableSet()
        val fallback = setOf(
            "doubleclick.net", "ad.google.com", "mc.yandex.ru", "an.yandex.ru",
            "videoroll.net", "marketgid.com", "mgid.com", "googleadservices.com"
        )
        finalBlockRules.addAll(fallback)

        // 2. Update Block Rules
        val (newBlockRoot, newBlockRegex, blockCount) = parseRules(finalBlockRules)
        root = newBlockRoot
        regexRules.clear()
        regexRules.addAll(newBlockRegex)
        ruleCount = blockCount

        // 2. Update Exception Rules
        val (newExceptionRoot, newExceptionRegex, excCount) = parseRules(filterData.exceptionRules)
        exceptionRoot = newExceptionRoot
        exceptionRegexRules.clear()
        exceptionRegexRules.addAll(newExceptionRegex)
        exceptionCount = excCount

        android.util.Log.i("FilterEngine", "Engine updated. Rules: $blockCount, Exceptions: $excCount")
    }

    private data class ParseResult(val root: TrieNode, val regexes: List<Regex>, val count: Int)

    private fun parseRules(rules: Set<String>): ParseResult {
        val newRoot = TrieNode()
        val newRegexList = mutableListOf<Regex>()
        var count = 0
        
        for (rule in rules) {
            val cleanRule = rule.trim().lowercase()
            if (cleanRule.isEmpty() || cleanRule.startsWith("#") || cleanRule.startsWith("!")) continue

            if (cleanRule.contains("*") || cleanRule.contains("^") || cleanRule.contains("|") || cleanRule.startsWith("/")) {
                // REGEX HARDENING: DNS filtering is heavy-handed. 
                // Skip rules with wildcards unless they are sufficiently long/precise.
                // This avoids collateral damage from broad rules like "cdn.*.*"
                if (cleanRule.contains("*") && cleanRule.length < 10) continue

                try {
                    val pattern = if (cleanRule.startsWith("/") && cleanRule.endsWith("/")) {
                        cleanRule.substring(1, cleanRule.length - 1)
                    } else {
                        cleanRule.replace(".", "\\.")
                                .replace("*", ".*")
                                .replace("^", "($|[:/\\?])")
                                .replace("||", "^(.*?\\.)?")
                                .replace("|", "\\|")
                    }
                    newRegexList.add(Regex(pattern))
                    count++
                } catch (e: Exception) {
                    // Log error but continue
                }
            } else {
                val labels = cleanRule.split('.').reversed()
                var current = newRoot
                for (label in labels) {
                    current = current.children.getOrPut(label) { TrieNode() }
                }
                current.isEndOfRule = true
                ruleMap[current] = cleanRule
                count++
            }
        }
        return ParseResult(newRoot, newRegexList, count)
    }

    fun getRuleCount(): Int = ruleCount

    enum class FilterStatus {
        BLOCKED,
        BLOCKED_USER,
        ALLOWED_USER,
        ALLOWED_SYSTEM,
        SUSPICIOUS, // Allowed but sketchy
        ALLOWED_DEFAULT
    }

    fun checkDomain(domain: String?): FilterStatus {
        if (domain.isNullOrBlank()) return FilterStatus.ALLOWED_DEFAULT
        val currentDomain = domain.lowercase().trim().trimEnd('.')
        
        // 1. Check for Exceptions (Allowlist) - Order: User > Static > Dynamic
        var temp = currentDomain
        while (temp.isNotEmpty()) {
            if (userAllowlist.contains(temp)) {
                return FilterStatus.ALLOWED_USER
            }
            if (allowlist.contains(temp)) {
                return FilterStatus.ALLOWED_SYSTEM
            }
            
            // Check dynamic exception Trie for this subdomain level
            val matchedException = checkTrie(exceptionRoot, temp)
            if (matchedException != null) {
                // Dynamic exceptions are usually from filter lists, treating as System for now unless we want a separate type
                return FilterStatus.ALLOWED_SYSTEM 
            }
            
            val nextDot = temp.indexOf('.')
            if (nextDot == -1) break
            temp = temp.substring(nextDot + 1)
        }
        
        // Dynamic Exception Regex
        val matchedExcRegex = exceptionRegexRules.find { it.containsMatchIn(currentDomain) }
        if (matchedExcRegex != null) {
            return FilterStatus.ALLOWED_SYSTEM
        }

        // 2. Check for User Blocklist (Manual Ban)
        temp = currentDomain
        while (temp.isNotEmpty()) {
            if (userBlocklist.contains(temp)) {
                return FilterStatus.BLOCKED_USER
            }
            val nextDot = temp.indexOf('.')
            if (nextDot == -1) break
            temp = temp.substring(nextDot + 1)
        }
        
        // 3. Check for Block Rules
        temp = currentDomain
        while (temp.isNotEmpty()) {
            val matchedRule = checkTrie(root, temp)
            if (matchedRule != null) {
                return FilterStatus.BLOCKED
            }
            val nextDot = temp.indexOf('.')
            if (nextDot == -1) break
            temp = temp.substring(nextDot + 1)
        }
        
        // Block Regex
        val matchedRegex = regexRules.find { it.containsMatchIn(currentDomain) }
        if (matchedRegex != null) {
            return FilterStatus.BLOCKED
        }
        
        // 4. Heuristics Check (Suspicious?)
        if (analyzeHeuristics(currentDomain)) {
            return FilterStatus.SUSPICIOUS
        }
        
        return FilterStatus.ALLOWED_DEFAULT
    }

    private fun analyzeHeuristics(domain: String): Boolean {
        // 1. Keywords check
        val keywords = listOf(
            "tracker", "analytics", "pixel", "telemetry", "stats", "metrics",
            "adsystem", "adserver", "banner", "campaign"
        )
        if (keywords.any { domain.contains(it) }) return true

        // 2. Entropy / Gibberish check (High digit count)
        // e.g. "a1b2c3d4e5.cdn.com"
        val parts = domain.split('.')
        for (part in parts) {
            // Ignore common parts like "www" or tlds like "com" (simple heuristic)
            if (part.length < 4) continue 
            
            val digitCount = part.count { it.isDigit() }
            val length = part.length
            
            // If more than 30% of a long-ish label are digits -> Suspicious
            if (length > 5 && (digitCount.toFloat() / length.toFloat() > 0.3)) return true
            
            // Or usually long random strings
            if (length > 35) return true
        }
        
        return false
    }

    fun shouldBlock(domain: String?): Boolean {
        val status = checkDomain(domain)
        return status == FilterStatus.BLOCKED || status == FilterStatus.BLOCKED_USER
    }

    private fun checkTrie(targetRoot: TrieNode, domain: String): String? {
        val labels = domain.split('.').reversed()
        var current = targetRoot
        for (label in labels) {
            current = current.children[label] ?: return null
            if (current.isEndOfRule) return ruleMap[current]
        }
        return null
    }
}
