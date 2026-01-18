package com.example.adshield.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import com.example.adshield.ui.theme.AppTheme

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("adshield_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXCLUDED_APPS = "excluded_apps"
        private const val KEY_USER_ALLOWLIST = "user_allowlist"
        private const val KEY_USER_BLOCKLIST = "user_blocklist"
        private const val KEY_FILTER_SOURCE = "filter_source_url"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_TRIAL_CONSUMED = "trial_consumed"
        private const val KEY_LAST_FILTER_UPDATE = "last_filter_update"
    }

    fun setTrialConsumed(consumed: Boolean) {
        prefs.edit { putBoolean(KEY_TRIAL_CONSUMED, consumed) }
    }

    fun isTrialConsumed(): Boolean {
        return prefs.getBoolean(KEY_TRIAL_CONSUMED, false)
    }

    fun getLastFilterUpdate(): Long {
        return prefs.getLong(KEY_LAST_FILTER_UPDATE, 0)
    }

    fun setLastFilterUpdate(timestamp: Long) {
        prefs.edit { putLong(KEY_LAST_FILTER_UPDATE, timestamp) }
    }

    fun setOnboardingComplete(complete: Boolean) {
        prefs.edit { putBoolean(KEY_ONBOARDING_COMPLETE, complete) }
    }

    fun isOnboardingComplete(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false)
    }

    fun resetOnboarding() {
        prefs.edit {
            remove(KEY_ONBOARDING_COMPLETE)
            remove(KEY_TRIAL_CONSUMED)
        }
    }

    fun getExcludedApps(): Set<String> {
        return prefs.getStringSet(KEY_EXCLUDED_APPS, emptySet()) ?: emptySet()
    }

    fun addExcludedApp(packageName: String) {
        val current = getExcludedApps().toMutableSet()
        current.add(packageName)
        prefs.edit { putStringSet(KEY_EXCLUDED_APPS, current) }
    }

    fun removeExcludedApp(packageName: String) {
        val current = getExcludedApps().toMutableSet()
        current.remove(packageName)
        prefs.edit { putStringSet(KEY_EXCLUDED_APPS, current) }
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
        prefs.edit { putStringSet(KEY_USER_ALLOWLIST, current) }
    }

    fun removeFromUserAllowlist(domain: String) {
        val current = getUserAllowlist().toMutableSet()
        current.remove(domain.lowercase())
        prefs.edit { putStringSet(KEY_USER_ALLOWLIST, current) }
    }

    // --- User-Defined Domain Blocklist ---

    fun getUserBlocklist(): Set<String> {
        return prefs.getStringSet(KEY_USER_BLOCKLIST, emptySet()) ?: emptySet()
    }

    fun addToUserBlocklist(domain: String) {
        val current = getUserBlocklist().toMutableSet()
        current.add(domain.lowercase())
        prefs.edit { putStringSet(KEY_USER_BLOCKLIST, current) }
    }

    fun removeFromUserBlocklist(domain: String) {
        val current = getUserBlocklist().toMutableSet()
        current.remove(domain.lowercase())
        prefs.edit { putStringSet(KEY_USER_BLOCKLIST, current) }
    }

    // --- Blocklist Source Configuration ---

    fun getFilterSourceUrl(): String {
        // Default to AdShield Custom Blocklist if not set
        return prefs.getString(
            KEY_FILTER_SOURCE,
            AppConfig.DEFAULT_FILTER_URL
        )
            ?: AppConfig.DEFAULT_FILTER_URL
    }

    fun setFilterSourceUrl(url: String) {
        prefs.edit { putString(KEY_FILTER_SOURCE, url) }
    }

    // --- Theme Configuration ---

    fun getAppTheme(): com.example.adshield.ui.theme.AppTheme {
        val themeName =
            prefs.getString(KEY_APP_THEME, AppConfig.DEFAULT_THEME.name)
        return try {
            com.example.adshield.ui.theme.AppTheme.valueOf(
                themeName ?: AppConfig.DEFAULT_THEME.name
            )
        } catch (_: Exception) {
            AppConfig.DEFAULT_THEME
        }
    }

    fun setAppTheme(theme: com.example.adshield.ui.theme.AppTheme) {
        prefs.edit { putString(KEY_APP_THEME, theme.name) }
    }

    val themeFlow: Flow<AppTheme> = callbackFlow {
        // Emit initial value
        trySend(getAppTheme())

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_APP_THEME) {
                trySend(getAppTheme())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
