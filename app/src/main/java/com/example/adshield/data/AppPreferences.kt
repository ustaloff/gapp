package com.example.adshield.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("adshield_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXCLUDED_APPS = "excluded_apps"
        private const val KEY_USER_ALLOWLIST = "user_allowlist"
        private const val KEY_USER_BLOCKLIST = "user_blocklist"
        private const val KEY_FILTER_SOURCE = "filter_source_url"
        private const val KEY_APP_THEME = "app_theme"
    }

    fun getExcludedApps(): Set<String> {
        return prefs.getStringSet(KEY_EXCLUDED_APPS, emptySet()) ?: emptySet()
    }

    fun addExcludedApp(packageName: String) {
        val current = getExcludedApps().toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet(KEY_EXCLUDED_APPS, current).apply()
    }

    fun removeExcludedApp(packageName: String) {
        val current = getExcludedApps().toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet(KEY_EXCLUDED_APPS, current).apply()
    }

    fun isAppExcluded(packageName: String): Boolean {
        return getExcludedApps().contains(packageName)
    }

    // --- User-Defined Domain Allowlist ---

    fun getUserAllowlist(): Set<String> {
        return prefs.getStringSet(KEY_USER_ALLOWLIST, emptySet()) ?: emptySet()
    }

    fun addToUserAllowlist(domain: String) {
        val current = getUserAllowlist().toMutableSet()
        current.add(domain.lowercase())
        prefs.edit().putStringSet(KEY_USER_ALLOWLIST, current).apply()
    }

    fun removeFromUserAllowlist(domain: String) {
        val current = getUserAllowlist().toMutableSet()
        current.remove(domain.lowercase())
        prefs.edit().putStringSet(KEY_USER_ALLOWLIST, current).apply()
    }

    // --- User-Defined Domain Blocklist ---

    fun getUserBlocklist(): Set<String> {
        return prefs.getStringSet(KEY_USER_BLOCKLIST, emptySet()) ?: emptySet()
    }

    fun addToUserBlocklist(domain: String) {
        val current = getUserBlocklist().toMutableSet()
        current.add(domain.lowercase())
        prefs.edit().putStringSet(KEY_USER_BLOCKLIST, current).apply()
    }

    fun removeFromUserBlocklist(domain: String) {
        val current = getUserBlocklist().toMutableSet()
        current.remove(domain.lowercase())
        prefs.edit().putStringSet(KEY_USER_BLOCKLIST, current).apply()
    }

    // --- Blocklist Source Configuration ---

    fun getFilterSourceUrl(): String {
        // Default to AdShield Custom Blocklist if not set
        return prefs.getString(
            KEY_FILTER_SOURCE,
            "https://raw.githubusercontent.com/ustaloff/adshield-lists/refs/heads/master/blocklist.txt"
        )
            ?: "https://raw.githubusercontent.com/ustaloff/adshield-lists/refs/heads/master/blocklist.txt"
    }

    fun setFilterSourceUrl(url: String) {
        prefs.edit().putString(KEY_FILTER_SOURCE, url).apply()
    }

    // --- Theme Configuration ---

    fun getAppTheme(): com.example.adshield.ui.theme.AppTheme {
        val themeName =
            prefs.getString(KEY_APP_THEME, com.example.adshield.ui.theme.AppTheme.CyberGreen.name)
        return try {
            com.example.adshield.ui.theme.AppTheme.valueOf(
                themeName ?: com.example.adshield.ui.theme.AppTheme.CyberGreen.name
            )
        } catch (e: Exception) {
            com.example.adshield.ui.theme.AppTheme.CyberGreen
        }
    }

    fun setAppTheme(theme: com.example.adshield.ui.theme.AppTheme) {
        prefs.edit().putString(KEY_APP_THEME, theme.name).apply()
    }
}
