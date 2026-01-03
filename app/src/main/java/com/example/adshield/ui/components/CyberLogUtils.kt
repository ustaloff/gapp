package com.example.adshield.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.adshield.filter.FilterEngine
import com.example.adshield.ui.theme.AdShieldTheme

data class LogStyle(
    val color: Color,
    val prefix: String,
    val isClickable: Boolean
)

@Composable
fun getLogStyle(status: FilterEngine.FilterStatus): LogStyle {
    return when (status) {
        FilterEngine.FilterStatus.BLOCKED -> LogStyle(MaterialTheme.colorScheme.error, "BLK", true)
        FilterEngine.FilterStatus.BLOCKED_USER -> LogStyle(
            AdShieldTheme.colors.warning,
            "BAN",
            true
        )

        FilterEngine.FilterStatus.ALLOWED_USER -> LogStyle(
            MaterialTheme.colorScheme.primary,
            "USR",
            true
        )

        FilterEngine.FilterStatus.ALLOWED_SYSTEM -> LogStyle(Color.Gray, "SYS", false)
        FilterEngine.FilterStatus.SUSPICIOUS -> LogStyle(AdShieldTheme.colors.warning, "WRN", true)
        FilterEngine.FilterStatus.ALLOWED_DEFAULT -> LogStyle(Color.White, "ALW", true)
        else -> LogStyle(Color.Gray, "UNK", false)
    }
}
