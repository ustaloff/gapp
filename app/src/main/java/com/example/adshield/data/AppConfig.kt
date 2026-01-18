package com.example.adshield.data

import com.example.adshield.ui.theme.AppTheme

object AppConfig {
    const val TRIAL_DURATION_DAYS = 21
    const val FREE_WHITELIST_LIMIT = 3
    val DEFAULT_THEME = AppTheme.CyberGreen
    const val DEFAULT_FILTER_URL =
        "https://raw.githubusercontent.com/ustaloff/adshield-lists/refs/heads/master/blocklist.txt"
    const val FILTER_UPDATE_COOLDOWN_HOURS = 24

    // Network & Safety Limits
    const val DOH_PRIMARY_URL = "https://cloudflare-dns.com/dns-query"
    const val DOH_BACKUP_URL = "https://dns.google/dns-query"
    const val DNS_TIMEOUT_MS = 2000
    const val FILTER_DOWNLOAD_TIMEOUT_MS = 5000
    const val FILTER_MAX_SIZE_BYTES = 5 * 1024 * 1024 // 5MB
}
