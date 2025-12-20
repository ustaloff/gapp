package com.example.adshield.data

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
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
    
    // Mutex to protect concurrent updates from multiple threads
    private val statsLock = Mutex()
    
    // Live stream of logs for the UI
    private val _recentLogs = mutableStateListOf<VpnLogEntry>()
    val recentLogs: List<VpnLogEntry> get() = _recentLogs

    // History for the graph (last 10 buckets, e.g. minutes)
    private val _blockedHistory = mutableStateListOf<Int>().apply { 
        repeat(12) { add(0) } 
    }
    val blockedHistory: List<Int> get() = _blockedHistory

    private var lastMinute = System.currentTimeMillis() / 60000

    fun setStatus(running: Boolean) {
        isRunning.value = running
        if (!running) {
            _recentLogs.clear()
            repeat(12) { _blockedHistory[it] = 0 }
        }
    }

    suspend fun incrementBlocked(domain: String) {
        statsLock.withLock {
            blockedCount.value++
            updateHistory()
            _blockedHistory[11]++
            addLog(domain, true)
        }
    }

    suspend fun incrementTotal(domain: String) {
        statsLock.withLock {
            totalCount.value++
            updateHistory()
            addLog(domain, false)
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

    private fun addLog(domain: String, isBlocked: Boolean) {
        _recentLogs.add(0, VpnLogEntry(System.currentTimeMillis(), domain, isBlocked))
        if (_recentLogs.size > 50) {
            _recentLogs.removeAt(_recentLogs.size - 1)
        }
    }
}
