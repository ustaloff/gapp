package com.example.adshield.data

import android.content.Context
import android.util.Log
import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
// import com.revenuecat.purchases.* // Disabled for Offline Mode

object BillingManager {
    
    // OFFLINE MODE: No API Key needed
    // private const val API_KEY = "..." 
    
    // State to observe in UI
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()
    
    // Mock offerings for UI
    data class MockPackage(val identifier: String, val product: MockProduct)
    data class MockProduct(val price: String, val title: String, val description: String)
    
    private val _currentOfferings = MutableStateFlow<List<MockPackage>>(emptyList())
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
        _isPremium.value = prefs.getBoolean("is_premium_unlocked", false)
        Log.i("BillingManager", "Offline Mode Initialized. Premium: ${_isPremium.value}")
    }

    fun purchase(activity: Activity, packageToBuy: Any?, onLoaders: (Boolean) -> Unit) {
        onLoaders(true)
        // Simulate network delay
        kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(1000)
            val prefs = activity.getSharedPreferences("adshield_prefs", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("is_premium_unlocked", true).apply()
            _isPremium.value = true
            
            // Sync with Firestore (Keep existing logic)
             kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                 UserRepository.updatePremiumStatus(true)
            }
            
            onLoaders(false)
            Log.i("BillingManager", "Offline Purchase Successful")
        }
    }

    fun restorePurchases(context: Context, onLoaders: (Boolean) -> Unit) {
         onLoaders(true)
         kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
            kotlinx.coroutines.delay(1000)
            val prefs = context.getSharedPreferences("adshield_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("is_premium_unlocked", false)) {
                 _isPremium.value = true
            }
            onLoaders(false)
         }
    }
    
    // Helper to manually set premium (Debug/Admin)
    fun setPremiumStatus(context: Context, isPro: Boolean) {
        val prefs = context.getSharedPreferences("adshield_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("is_premium_unlocked", isPro).apply()
        _isPremium.value = isPro
    }
}
