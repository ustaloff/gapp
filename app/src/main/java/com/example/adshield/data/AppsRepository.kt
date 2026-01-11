package com.example.adshield.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable,
    val isSystem: Boolean
)

class AppsRepository(private val context: Context) {

    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        apps.map { appInfo ->
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            AppInfo(
                name = pm.getApplicationLabel(appInfo).toString(),
                packageName = appInfo.packageName,
                icon = pm.getApplicationIcon(appInfo),
                isSystem = isSystem
            )
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun getEffectiveWhitelist(
        excludedApps: Set<String>,
        isUnlimited: Boolean
    ): Set<String> = withContext(Dispatchers.IO) {
        if (isUnlimited) {
            excludedApps
        } else {
            val pm = context.packageManager
            excludedApps.map { pkg ->
                val label = try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    pkg
                }
                pkg to label
            }
                .sortedBy { it.second.lowercase() }
                .take(AppConfig.FREE_WHITELIST_LIMIT)
                .map { it.first }
                .toSet()
        }
    }
}
