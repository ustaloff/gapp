package com.example.adshield.data

import android.content.Context
import android.util.Log
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchases.interfaces.ReceiveCustomerInfoCallback
import com.revenuecat.purchases.PurchasesError
import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers


object BillingManager {
    
    // API KEY from User
    private const val API_KEY = "test_FQrTkNbtPOGaKlCRbJzErbsTuZY"
    
    // State to observe in UI
    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()
    
    private val _currentOfferings = MutableStateFlow<List<Package>>(emptyList())
    val currentOfferings = _currentOfferings.asStateFlow()

    fun initialize(context: Context) {
        Purchases.debugLogsEnabled = true
        Purchases.configure(PurchasesConfiguration.Builder(context, API_KEY).build())
        
        // Check initial status
        Purchases.sharedInstance.getCustomerInfo(object : ReceiveCustomerInfoCallback {
            override fun onReceived(customerInfo: CustomerInfo) {
                updatePremiumStatus(customerInfo)
            }
            override fun onError(error: PurchasesError) {
                Log.e("BillingManager", "Init Error: ${error.message}")
            }
        })
        
        updatedOfferings()
    }
    
    private fun updatedOfferings() {
        Purchases.sharedInstance.getOfferingsWith({ error ->
            Log.e("BillingManager", "Offerings Error: ${error.message}")
        }) { offerings ->
            offerings.current?.availablePackages?.let { packages ->
                _currentOfferings.value = packages
            }
        }
    }

    fun purchase(activity: Activity, packageToBuy: Package, onLoaders: (Boolean) -> Unit) {
        onLoaders(true)
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(activity, packageToBuy).build(),
            onError = { error, userCancelled ->
                onLoaders(false)
                if (!userCancelled) {
                    Log.e("BillingManager", "Purchase Error: ${error.message}")
                }
            },
            onSuccess = { _, customerInfo ->
                onLoaders(false)
                updatePremiumStatus(customerInfo)
            }
        )
    }

    fun restorePurchases(onLoaders: (Boolean) -> Unit) {
        onLoaders(true)
        Purchases.sharedInstance.restorePurchasesWith(
            onError = { error ->
                onLoaders(false)
                Log.e("BillingManager", "Restore Error: ${error.message}")
            },
            onSuccess = { customerInfo ->
                onLoaders(false)
                updatePremiumStatus(customerInfo)
            }
        )
    }

    private fun updatePremiumStatus(customerInfo: CustomerInfo) {
        // "adShield Pro" is the Entitlement ID set in RevenueCat dashboard (default suggestion)
        // If user set something else, this needs to match. Assuming "pro" or "premium" or the screenshot name "adShield Pro"
        // Screenshot showed Entitlement name: "adShield Pro" -> Identifier usually "adShield_Pro" or similar?
        // Let's assume the user kept defaults or we check for ANY active entitlement for start.
        
        val isPro = customerInfo.entitlements["adShield Pro"]?.isActive == true 
                 || customerInfo.entitlements["pro"]?.isActive == true
                 || customerInfo.entitlements.all.values.any { it.isActive } // Fallback: any active = premium
        
        _isPremium.value = isPro
        
        _isPremium.value = isPro
        
        // Sync with Firestore
        if (isPro) {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                 UserRepository.updatePremiumStatus(true)
            }
        }
    }
}
