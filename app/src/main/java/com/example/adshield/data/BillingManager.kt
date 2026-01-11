package com.example.adshield.data

import android.content.Context
import android.util.Log
import android.app.Activity
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.example.adshield.ui.theme.AppTheme
import com.example.adshield.data.AppConfig

// import com.revenuecat.purchases.* // Disabled for Offline Mode

object BillingManager {

    // OFFLINE MODE: No API Key needed
    // private const val API_KEY = "..." 

    // State to observe in UI (Internal source of truth is UserAccess object)
    private val _userAccess = MutableStateFlow(UserAccess(UserAccessState.FREE))
    
    // Expose just the state enum for UI compatibility as a Flow
    val userAccessState = _userAccess.map { it.state }
    
    // Valid Public Accessor for UI (includes Timestamps)
    val currentUserAccess = _userAccess.asStateFlow()

    // Exposed Entitlements for easy consumption
    val entitlements = _userAccess.map { AccessControl.entitlementsFor(it.state) }
    
    fun getCurrentEntitlements(): UserEntitlements = AccessControl.entitlementsFor(_userAccess.value.state)

    // Backward compatibility helpers (temporary)
    val isPremium: kotlinx.coroutines.flow.Flow<Boolean> = _userAccess.map { 
        it.state == UserAccessState.PREMIUM || it.state == UserAccessState.TRIAL 
    }
    
    // Check if user is eligible for trial (not consumed)
    fun isTrialEligible(context: Context): Boolean {
        return !AppPreferences(context).isTrialConsumed()
    }
    
    // Mock offerings for UI (Public types)
    data class MockPackage(val identifier: String, val product: MockProduct)
    data class MockProduct(val price: String, val title: String, val description: String)


    // Mock offerings for UI

    // We use Any to avoid depending on RC Package, but UI expects Package.
    // To avoid breaking UI imports, we might need a wrapper or just mock it carefully.
    // UI uses `packageToBuy: Package` in `purchase`.
    // We should probably strip `Package` type from the UI arguments if we remove the library.
    // But `BillingManager` signature change requires updating `PremiumScreen`.
    // Let's check imports in `PremiumScreen` later.
    // For now, I'll comment out RevenueCat and change signatures to `Any` or specific mock types? 
    // No, that breaks compilation of `PremiumScreen`.
    // SAFEST ADJUSTMENT:
    // Keep RevenueCat imports for compilation if library is still in gradle?
    // User wants "Fix it". 
    // If I remove the library from Gradle -> compilation breaks everywhere.
    // If I keep the library -> I can use the types but NOT `Purchases.configure`.

    // Plan: 
    // 1. Keep imports for types (Package, etc.) so UI doesn't break.
    // 2. Disable `Purchases.configure` logic.
    // 3. Mock `purchase` to succeed immediately.

    // private val _currentOfferings = MutableStateFlow<List<Package>>(emptyList()) 
    // Since we can't easily instantiate a RevenueCat `Package` (it has private constructors etc?), 
    // we might have to clean up `PremiumScreen`.

    // Let's assume for this step I WILL break `PremiumScreen` if I remove imports.
    // So I must fix `PremiumScreen` next.
    // I will simplify `BillingManager` to specific simple types.

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences("adshield_prefs", Context.MODE_PRIVATE)
        val savedStateName = prefs.getString("user_access_state", UserAccessState.FREE.name)
        val trialEndsAt = if (prefs.contains("trial_ends_at")) prefs.getLong("trial_ends_at", 0) else null
        val premiumExpiresAt = if (prefs.contains("premium_expires_at")) prefs.getLong("premium_expires_at", 0) else null

        val loadedState = try {
            UserAccessState.valueOf(savedStateName ?: UserAccessState.FREE.name)
        } catch (_: Exception) {
            UserAccessState.FREE
        }
        
        val access = UserAccess(loadedState, trialEndsAt, premiumExpiresAt)
        
        // Check expiration immediately on init
        if (access.isExpired()) {
             Log.i("BillingManager", "Access Expired. Downgrading to FREE.")
             updateState(context, UserAccess(UserAccessState.FREE)) 
        } else {
             _userAccess.value = access
             checkAndEnforceTheme(context, access.state)
        }
        Log.i("BillingManager", "Offline Mode Initialized. State: ${_userAccess.value.state}")
    }

    fun activateTrial(context: Context) {
        val endsAt = System.currentTimeMillis() + (AppConfig.TRIAL_DURATION_DAYS * 24 * 60 * 60 * 1000L)
        val access = UserAccess(UserAccessState.TRIAL, trialEndsAt = endsAt)
        
        // Mark trial as consumed forever
        AppPreferences(context).setTrialConsumed(true)
        
        updateState(context, access)
    }

    fun activatePremium(context: Context) {
        // Lifetime by default for now unless we add expiration logic to packages
        val access = UserAccess(UserAccessState.PREMIUM, premiumExpiresAt = null)
        updateState(context, access)
    }
    
    fun resetToFree(context: Context) {
         updateState(context, UserAccess(UserAccessState.FREE))
    }

    private fun updateState(context: Context, newAccess: UserAccess) {
        _userAccess.value = newAccess
        
        checkAndEnforceTheme(context, newAccess.state)

        
        context.getSharedPreferences("adshield_prefs", Context.MODE_PRIVATE).edit {
            putString("user_access_state", newAccess.state.name)
             if (newAccess.trialEndsAt != null) putLong("trial_ends_at", newAccess.trialEndsAt) else remove("trial_ends_at")
            if (newAccess.premiumExpiresAt != null) putLong("premium_expires_at", newAccess.premiumExpiresAt) else remove("premium_expires_at")
        }
        
        val isEffectivePremium = (newAccess.state == UserAccessState.PREMIUM || newAccess.state == UserAccessState.TRIAL)
        CoroutineScope(Dispatchers.IO).launch {
             // Sync full state to cloud
             UserRepository.updateUserAccess(newAccess)
        }
    }

    fun purchase(activity: Activity, @Suppress("UNUSED_PARAMETER") packageToBuy: Any?, onLoaders: (Boolean) -> Unit) {
        onLoaders(true)
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            activatePremium(activity)
            onLoaders(false)
            Log.i("BillingManager", "Offline Purchase Successful")
        }
    }

    fun restorePurchases(context: Context, onLoaders: (Boolean) -> Unit) {
        onLoaders(true)
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
             // Re-run initialize logic effectively or valid check
             // For offline mock, just re-read prefs? 
             // Ideally we check with server. For now, trust local prefs and check expiration.
             initialize(context)
             onLoaders(false)
        }
    }


    private fun checkAndEnforceTheme(context: Context, state: UserAccessState) {
        if (state == UserAccessState.FREE) {
             val prefs = AppPreferences(context)
             val currentTheme = prefs.getAppTheme()
             if (currentTheme.isPremium) {
                 prefs.setAppTheme(AppConfig.DEFAULT_THEME)
                 Log.i("BillingManager", "Enforcing Default Theme for FREE user.")
             }
        }
    }
}
