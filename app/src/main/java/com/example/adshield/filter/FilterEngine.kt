package com.example.adshield.filter

object FilterEngine {

    private class TrieNode {
        val children = mutableMapOf<String, TrieNode>()
        var isEndOfRule = false
    }

    private var root = TrieNode()
    private var ruleCount = 0
    private val regexRules = mutableListOf<Regex>()
    
    // Safety Allowlist - Simple set is fine for small count
    private val allowlist = setOf(
        "facebook.com", "www.facebook.com", "twitch.tv", "www.twitch.tv", "ttvnw.net",
        "youtube.com", "www.youtube.com", "googlevideo.com",
        "hdrezka.ac", "hdrezka.ag", "hdrezka.me", "hdrezka.co", "hdrezka.re", "hdrezka-home.tv",
        "rezka.ag", "rezka.me", "static.hdrezka.ac", "hls.hdrezka.ac",
        "voidboost.net", "voidboost.cc",
        "google.com", "googleapis.com", "gstatic.com", "amazon.com", "amazonaws.com",
        "cloudfront.net", "cdn.net", "cloudflare.com", "microsoft.com", "apple.com",
        "gvt1.com", "gvt2.com", "gvt3.com",
        "ss.lv", "m.ss.lv", "www.ss.lv", "inbox.lv"
    )

    init {
        // Load fallback rules into the Trie
        val fallback = listOf(
            "doubleclick.net", "ad.google.com", "mc.yandex.ru", "an.yandex.ru",
            "videoroll.net", "marketgid.com", "mgid.com", "googleadservices.com"
        )
        updateBlocklist(fallback.toSet())
    }

    @Synchronized
    fun updateBlocklist(newRules: Set<String>) {
        if (newRules.isEmpty()) return
        
        val newRoot = TrieNode()
        val newRegexList = mutableListOf<Regex>()
        var newCount = 0
        
        for (rule in newRules) {
            val cleanRule = rule.trim().lowercase()
            if (cleanRule.isEmpty() || cleanRule.startsWith("#") || cleanRule.startsWith("!")) continue

            // 1. Identify Regex/Pattern rules
            if (cleanRule.contains("*") || cleanRule.startsWith("/") || cleanRule.contains("^")) {
                try {
                    val pattern = if (cleanRule.startsWith("/") && cleanRule.endsWith("/")) {
                        cleanRule.substring(1, cleanRule.length - 1)
                    } else {
                        // Basic conversion of AdBlock style to Regex
                        cleanRule.replace(".", "\\.")
                                .replace("*", ".*")
                                .replace("^", "($|[:/\\?])")
                                .replace("||", "^(.*?\\.)?")
                    }
                    newRegexList.add(Regex(pattern))
                    newCount++
                } catch (e: Exception) {
                    android.util.Log.w("FilterEngine", "Invalid regex skipped: $cleanRule")
                }
            } else {
                // 2. Standard Domain -> Trie
                val labels = cleanRule.split('.').reversed()
                var current = newRoot
                for (label in labels) {
                    current = current.children.getOrPut(label) { TrieNode() }
                }
                current.isEndOfRule = true
                newCount++
            }
        }
        
        root = newRoot
        regexRules.clear()
        regexRules.addAll(newRegexList)
        ruleCount = newCount
        android.util.Log.i("FilterEngine", "Engine updated. Total rules: $newCount (Regex: ${newRegexList.size})")
    }

    fun getRuleCount(): Int = ruleCount

    /**
     * Advanced hybrid matching: Trie + Regex.
     */
    fun shouldBlock(domain: String?): Boolean {
        if (domain.isNullOrBlank()) return false

        val currentDomain = domain.lowercase()
        
        // 1. Safety Check: Allowlist
        var tempDomain = currentDomain
        while (tempDomain.isNotEmpty()) {
            if (allowlist.contains(tempDomain)) return false
            val dotIndex = tempDomain.indexOf('.')
            if (dotIndex == -1) break
            tempDomain = tempDomain.substring(dotIndex + 1)
        }

        // 2. Trie Matching (Standard domains) - O(Labels)
        val labels = currentDomain.split('.').reversed()
        var current = root
        for (label in labels) {
            current = current.children[label] ?: break
            if (current.isEndOfRule) {
                android.util.Log.i("FilterEngine", "BLOCKED (Trie): $domain")
                return true
            }
        }
        
        // 3. Regex Matching (Advanced patterns) - O(Rules * Complexity)
        // We only check regex if trie didn't match.
        for (regex in regexRules) {
            if (regex.containsMatchIn(currentDomain)) {
                android.util.Log.i("FilterEngine", "BLOCKED (Regex): $domain matched ${regex.pattern}")
                return true
            }
        }
        
        return false
    }
}
