package com.example.adshield.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object VpnStats {
    private val _blockedCount = MutableStateFlow(0)
    val blockedCount = _blockedCount.asStateFlow()

    private val _totalRequests = MutableStateFlow(0)
    val totalRequests = _totalRequests.asStateFlow()

    val isRunning = MutableStateFlow(false) // Changed to MutableStateFlow

    fun incrementBlocked() {
        _blockedCount.value += 1
        _totalRequests.value += 1
    }

    fun incrementTotal() {
        _totalRequests.value += 1
    }
    
    fun setStatus(running: Boolean) {
        isRunning.value = running
    }
}
