package com.example.adshield.data

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class VpnLogEntry(
    val timestamp: Long,
    val domain: String,
    val isBlocked: Boolean,
    val appName: String? = null
)

object VpnStats {
    val isRunning = mutableStateOf(false)
    val blockedCount = mutableStateOf(0)
    val totalCount = mutableStateOf(0)
    val dataSavedBytes = mutableStateOf(0L)
    
    val blockedToday = mutableStateOf(0)
    val blockedWeekly = mutableStateOf(0) // Sum of last 7 days

    // Professional Metrics
    val blocksPerMinute = mutableStateOf(0)
    val growthToday = mutableStateOf(0) // Percentage vs yesterday
    val timeSavedMs = mutableStateOf(0L) // Estimated time saved in ms

    val appBlockedStats = mutableStateListOf<Pair<String, Int>>() // Simplified for sorting: actually let's use map and convert in UI, or stateMap
    val appBlockedStatsMap = mutableStateMapOf<String, Int>()
    val domainBlockedStatsMap = mutableStateMapOf<String, Int>()

    // Persistence
    private const val PREFS_NAME = "adshield_stats"
    private const val KEY_TOTAL = "total_blocked"
    private const val KEY_DAILY_COUNTS = "daily_counts_csv" // "10,5,0,0,0,0,0"
    private const val KEY_LAST_DAY = "last_day_index"
    private const val KEY_DATA_SAVED = "data_saved"
    private const val KEY_TIME_SAVED = "time_saved"

    // Daily buckets: index 0 is today, 1 is yesterday, etc.
    private val dailyBuckets = IntArray(7)



    // ...

    private fun updatePublicMetrics() {
        blockedToday.value = dailyBuckets[0]
        blockedWeekly.value = dailyBuckets.sum()
        
        // Calculate Growth (Today vs Yesterday)
        val today = dailyBuckets[0]
        val yesterday = dailyBuckets[1]
        
        if (yesterday > 0) {
            growthToday.value = ((today - yesterday).toFloat() / yesterday.toFloat() * 100).toInt()
        } else {
            // If yesterday was 0, growth is technically infinite, but let's cap it or just show 100% if today > 0
            growthToday.value = if (today > 0) 100 else 0
        }
    }


    // Mutex to protect concurrent updates from multiple threads
    private val statsLock = Mutex()
    
    // Live stream of logs for the UI
    private val _recentLogs = mutableStateListOf<VpnLogEntry>()
    val recentLogs: List<VpnLogEntry> get() = _recentLogs

    // History for the graph (last 60 buckets, 1 minute each)
    private val _blockedHistory = mutableStateListOf<Int>().apply { 
        repeat(60) { add(0) } 
    }
    val blockedHistory: List<Int> get() = _blockedHistory

    private var lastMinute = System.currentTimeMillis() / 60000

    fun setStatus(running: Boolean) {
        isRunning.value = running
        if (!running) {
            _recentLogs.clear()
            repeat(60) { _blockedHistory[it] = 0 }
            blocksPerMinute.value = 0
        }
    }

    fun initialize(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        blockedCount.value = prefs.getInt(KEY_TOTAL, 0)
        dataSavedBytes.value = prefs.getLong(KEY_DATA_SAVED, 0L)
        timeSavedMs.value = prefs.getLong(KEY_TIME_SAVED, 0L)
        
        val savedCounts = prefs.getString(KEY_DAILY_COUNTS, "") ?: ""
        if (savedCounts.isNotEmpty()) {
            val parts = savedCounts.split(",")
            for (i in parts.indices) {
                if (i < 7) dailyBuckets[i] = parts[i].toIntOrNull() ?: 0
            }
        }
        
        checkDayReset(context)
        updatePublicMetrics()
    }

    private fun checkDayReset(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val lastDay = prefs.getLong(KEY_LAST_DAY, 0L)
        val currentDay = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        
        if (currentDay > lastDay) {
            val daysPassed = (currentDay - lastDay).toInt()
            // Shift buckets
            if (daysPassed >= 7) {
                for (i in 0 until 7) dailyBuckets[i] = 0
            } else {
                for (i in 6 downTo daysPassed) {
                    dailyBuckets[i] = dailyBuckets[i - daysPassed]
                }
                for (i in 0 until daysPassed) {
                    dailyBuckets[i] = 0
                }
            }
            prefs.edit().putLong(KEY_LAST_DAY, currentDay).apply()
            saveStats(context)
        }
    }

    private fun saveStats(context: android.content.Context) {
         val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
         val csv = dailyBuckets.joinToString(",")
         prefs.edit()
             .putInt(KEY_TOTAL, blockedCount.value)
             .putLong(KEY_DATA_SAVED, dataSavedBytes.value)
             .putLong(KEY_TIME_SAVED, timeSavedMs.value)
             .putString(KEY_DAILY_COUNTS, csv)
             .apply()
    }



    suspend fun incrementBlocked(context: android.content.Context, domain: String, appName: String? = null) {
        statsLock.withLock {
            blockedCount.value++
            // Industry standard: 30KB saved per blocked ad
            dataSavedBytes.value += 30 * 1024 
            // Estimated time saved: 300ms per blocked resource
            timeSavedMs.value += 300
            
            checkDayReset(context)
            dailyBuckets[0]++
            saveStats(context)
            updatePublicMetrics() 
            
            updateHistory()
            _blockedHistory[59]++
            blocksPerMinute.value = _blockedHistory[59] // Live BPM update

            // Update Domain Stats
            domainBlockedStatsMap[domain] = (domainBlockedStatsMap[domain] ?: 0) + 1
            
            // Update App Stats
            if (appName != null) {
                 appBlockedStatsMap[appName] = (appBlockedStatsMap[appName] ?: 0) + 1
            }

            addLog(domain, true, appName)
        }
    }

    suspend fun incrementTotal(domain: String, appName: String? = null) {
        statsLock.withLock {
            totalCount.value++
            updateHistory()
            addLog(domain, false, appName)
        }
    }

    private fun updateHistory() {
        val currentMinute = System.currentTimeMillis() / 60000
        if (currentMinute > lastMinute) {
            val minutesPassed = (currentMinute - lastMinute).toInt()
            repeat(minutesPassed.coerceAtMost(12)) {
                _blockedHistory.removeAt(0)
                _blockedHistory.add(0)
            }
            lastMinute = currentMinute
        }
    }

    private fun addLog(domain: String, isBlocked: Boolean, appName: String?) {
        _recentLogs.add(0, VpnLogEntry(System.currentTimeMillis(), domain, isBlocked, appName))
        if (_recentLogs.size > 50) {
            _recentLogs.removeAt(_recentLogs.size - 1)
        }
    }
}
