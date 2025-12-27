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



    fun isDohBypass(domain: String): Boolean {
        val clean = domain.lowercase().trim().trimEnd('.')
        return FilterLists.dohBypassList.any { clean == it || clean.endsWith(".$it") }
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
        updateBlocklist(
            com.example.adshield.data.FilterRepository.FilterData(
                emptySet(),
                emptySet()
            )
        )
    }

    @Synchronized
    fun updateBlocklist(filterData: com.example.adshield.data.FilterRepository.FilterData) {
        // 1. Reset and Merge Fallback Rules
        ruleMap.clear()
        val finalBlockRules = filterData.blockRules.toMutableSet()
        val fallback = FilterLists.fallbackBlocklist
        finalBlockRules.addAll(fallback)
        android.util.Log.i("FilterEngine", "Merging ${fallback.size} fallback rules.")


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

        android.util.Log.i(
            "FilterEngine",
            "Engine updated. Rules: $blockCount, Exceptions: $excCount"
        )
    }

    private data class ParseResult(val root: TrieNode, val regexes: List<Regex>, val count: Int)

    private fun parseRules(rules: Set<String>): ParseResult {
        val newRoot = TrieNode()
        val newRegexList = mutableListOf<Regex>()
        var count = 0

        for (rule in rules) {
            val cleanRule = rule.trim().lowercase()
            if (cleanRule.isEmpty() || cleanRule.startsWith("#") || cleanRule.startsWith("!")) continue

            if (cleanRule.contains("*") || cleanRule.contains("^") || cleanRule.contains("|") || cleanRule.startsWith(
                    "/"
                )
            ) {
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

        // 1. Check for Explicit Overrides (User Rules > System Exceptions)
        // Order: User Allow > User Block > System/Dynamic Exceptions
        
        // Iterating subdomains without creating substring objects for the loop
        var startIndex = 0
        while (startIndex < currentDomain.length) {
            // We can't easily optimize the Sets checks (allowlist.contains) without substring
            // because Set keys are Strings. 
            // However, we CAN optimize the loop to only create the substring when needed for the Set lookup.
            val subDomain = currentDomain.substring(startIndex)

            if (userAllowlist.contains(subDomain)) {
                return FilterStatus.ALLOWED_USER
            }
            // User Blocklist (Moved Up: User Ban overrides System Allow)
            if (userBlocklist.contains(subDomain)) {
                return FilterStatus.BLOCKED_USER
            }

            // System/Dynamic Exceptions
            if (FilterLists.allowlist.contains(subDomain)) {
                return FilterStatus.ALLOWED_SYSTEM
            }

            // Dynamic exception Trie - OPTIMIZED to use startIndex
            val matchedException = checkTrie(exceptionRoot, currentDomain, startIndex)
            if (matchedException != null) {
                return FilterStatus.ALLOWED_SYSTEM
            }

            val nextDot = currentDomain.indexOf('.', startIndex)
            if (nextDot == -1) break
            startIndex = nextDot + 1
        }
        
        // Dynamic Exception Regex (Still slow, but less frequently hit)
        val matchedExcRegex = exceptionRegexRules.find { it.containsMatchIn(currentDomain) }
        if (matchedExcRegex != null) {
            return FilterStatus.ALLOWED_SYSTEM
        }

        // 2. Check for Block Rules (if not allowed/banned explicitly above)
        
        startIndex = 0
        while (startIndex < currentDomain.length) {
            // Trie Optimization: Pass full string + offset
            val matchedRule = checkTrie(root, currentDomain, startIndex)
            if (matchedRule != null) {
                return FilterStatus.BLOCKED
            }
            
            val nextDot = currentDomain.indexOf('.', startIndex)
            if (nextDot == -1) break
            startIndex = nextDot + 1
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

        // 2. Entropy / Gibberish check (High digit count) - ALLOCATION FREE LOOP
        var labelStart = 0
        val len = domain.length
        
        for (i in 0..len) {
            if (i == len || domain[i] == '.') {
                // Segment Logic
                val partLen = i - labelStart
                
                // Ignore common parts
                if (partLen > 3) { // optimization equivalent to "part.length < 4 continue"
                    // Count digits manually avoiding .count{} allocation
                    var digitCount = 0
                    for (j in labelStart until i) {
                         if (domain[j].isDigit()) digitCount++
                    }
                    
                     // If more than 30% of a long-ish label are digits -> Suspicious
                    if (partLen > 5 && (digitCount.toFloat() / partLen > 0.3)) return true

                    // Or usually long random strings
                    if (partLen > 35) return true
                }
                
                labelStart = i + 1
            }
        }

        return false
    }

    fun shouldBlock(domain: String?): Boolean {
        val status = checkDomain(domain)
        return status == FilterStatus.BLOCKED || status == FilterStatus.BLOCKED_USER
    }

    // OPTIMIZED: Traverses segments from right-to-left without splitting into a List
    private fun checkTrie(targetRoot: TrieNode, domain: String, startIndex: Int): String? {
        var end = domain.length
        var current = targetRoot
        
        // Scan backwards from end of string down to startIndex
        for (i in domain.length - 1 downTo startIndex) {
            if (domain[i] == '.') {
                // Found separator. The label is (i+1 .. end)
                if (i + 1 < end) {
                    val label = domain.substring(i + 1, end)
                    current = current.children[label] ?: return null
                    if (current.isEndOfRule) return ruleMap[current]
                }
                end = i
            }
        }
        
        // Process first label (from startIndex to first dot/end)
        if (startIndex < end) {
            val label = domain.substring(startIndex, end)
            current = current.children[label] ?: return null
            if (current.isEndOfRule) return ruleMap[current]
        }
        
        return null
    }
}
