package com.example.adshield.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("adshield_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_EXCLUDED_APPS = "excluded_apps"
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
}
