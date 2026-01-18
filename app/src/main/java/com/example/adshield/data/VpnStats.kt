package com.example.adshield.data

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class VpnLogEntry(
    val timestamp: Long,
    val domain: String,
    val status: com.example.adshield.filter.FilterEngine.FilterStatus,
    val appName: String? = null
)

object VpnStats {
    val isRunning = mutableStateOf(false)
    val blockedCount = mutableIntStateOf(0)
    val totalCount = mutableIntStateOf(0)
    val dataSavedBytes = mutableLongStateOf(0L)

    val blockedToday = mutableIntStateOf(0)
    val blockedWeekly = mutableIntStateOf(0) // Sum of last 7 days

    // Professional Metrics
    val blocksPerMinute = mutableIntStateOf(0)
    val growthToday = mutableIntStateOf(0) // Percentage vs yesterday
    val timeSavedMs = mutableLongStateOf(0L) // Estimated time saved in ms

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
        blockedToday.intValue = dailyBuckets[0]
        blockedWeekly.intValue = dailyBuckets.sum()

        // Calculate Growth (Today vs Yesterday)
        val today = dailyBuckets[0]
        val yesterday = dailyBuckets[1]

        if (yesterday > 0) {
            growthToday.intValue =
                ((today - yesterday).toFloat() / yesterday.toFloat() * 100).toInt()
        } else {
            // If yesterday was 0, growth is technically infinite, but let's cap it or just show 100% if today > 0
            growthToday.intValue = if (today > 0) 100 else 0
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

    // Total Requests History
    private val _totalHistory = mutableStateListOf<Int>().apply {
        repeat(60) { add(0) }
    }
    val totalHistory: List<Int> get() = _totalHistory

    private var lastMinute = System.currentTimeMillis() / 60000

    fun setStatus(running: Boolean) {
        isRunning.value = running
        if (!running) {
            _recentLogs.clear()
            repeat(60) {
                _blockedHistory[it] = 0
                _totalHistory[it] = 0
            }
            blocksPerMinute.intValue = 0
        }
    }

    fun initialize(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        blockedCount.intValue = prefs.getInt(KEY_TOTAL, 0)
        dataSavedBytes.longValue = prefs.getLong(KEY_DATA_SAVED, 0L)
        timeSavedMs.longValue = prefs.getLong(KEY_TIME_SAVED, 0L)

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
            prefs.edit { putLong(KEY_LAST_DAY, currentDay) }
            saveStats(context)
        }
    }

    private fun saveStats(context: android.content.Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val csv = dailyBuckets.joinToString(",")

        prefs.edit {
            putInt(KEY_TOTAL, blockedCount.intValue)
            putLong(KEY_DATA_SAVED, dataSavedBytes.longValue)
            putLong(KEY_TIME_SAVED, timeSavedMs.longValue)
            putString(KEY_DAILY_COUNTS, csv)
        }
    }

    suspend fun increment(
        context: android.content.Context,
        domain: String,
        status: com.example.adshield.filter.FilterEngine.FilterStatus,
        appName: String? = null
    ) {
        withContext(Dispatchers.Main) {
            statsLock.withLock {
                // ALWAYS increment current minute total
                _totalHistory[59]++

                if (status == com.example.adshield.filter.FilterEngine.FilterStatus.BLOCKED) {
                    blockedCount.intValue++
                    dataSavedBytes.longValue += 30 * 1024
                    timeSavedMs.longValue += 300
                    checkDayReset(context)
                    dailyBuckets[0]++
                    saveStats(context)
                    updatePublicMetrics()
                    updateHistory()
                    _blockedHistory[59]++
                    blocksPerMinute.intValue = _blockedHistory[59]
                } else {
                    totalCount.intValue++
                    updateHistory()
                }

                // Update Domain Stats
                if (status == com.example.adshield.filter.FilterEngine.FilterStatus.BLOCKED) {
                    domainBlockedStatsMap[domain] = (domainBlockedStatsMap[domain] ?: 0) + 1
                    if (appName != null) {
                        appBlockedStatsMap[appName] = (appBlockedStatsMap[appName] ?: 0) + 1
                    }
                }

                addLog(domain, status, appName)
            }
        }
    }


    private fun updateHistory() {
        val currentMinute = System.currentTimeMillis() / 60000
        if (currentMinute > lastMinute) {
            val minutesPassed = (currentMinute - lastMinute).toInt()
            repeat(minutesPassed.coerceAtMost(12)) {
                if (_blockedHistory.isNotEmpty()) {
                    _blockedHistory.removeAt(0)
                    _blockedHistory.add(0)
                }
                if (_totalHistory.isNotEmpty()) {
                    _totalHistory.removeAt(0)
                    _totalHistory.add(0)
                }
            }
            lastMinute = currentMinute
        }
    }

    private fun addLog(
        domain: String,
        status: com.example.adshield.filter.FilterEngine.FilterStatus,
        appName: String?
    ) {
        _recentLogs.add(0, VpnLogEntry(System.currentTimeMillis(), domain, status, appName))
        if (_recentLogs.size > 50) {
            _recentLogs.removeAt(_recentLogs.size - 1)
        }
    }

    fun refreshLogStatuses() {
        // Simple update on Main Thread (invoked by UI interaction)
        // We iterate and update. Since recentLogs is state-backed, it handles notification.
        // We assume we are on Main thread or it handles it.
        val currentList = _recentLogs.toList()
        _recentLogs.clear()
        currentList.forEach { log ->
            val newStatus = com.example.adshield.filter.FilterEngine.checkDomain(log.domain)
            _recentLogs.add(log.copy(status = newStatus))
        }
    }
}
