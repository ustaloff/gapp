package com.example.adshield.data

enum class UserAccessState {
    TRIAL,
    FREE,
    PREMIUM;

    /** Returns true if user is FREE (has limitations) */
    fun isFree(): Boolean = this == FREE

    /** Returns true if user has premium access (TRIAL or PREMIUM) */
    fun hasPremiumAccess(): Boolean = this != FREE
}

data class UserAccess(
    val state: UserAccessState,
    val trialEndsAt: Long? = null,
    val premiumExpiresAt: Long? = null
) {
    fun isExpired(): Boolean {
        val now = System.currentTimeMillis()
        return when (state) {
            UserAccessState.TRIAL -> trialEndsAt != null && now > trialEndsAt
            UserAccessState.PREMIUM -> premiumExpiresAt != null && now > premiumExpiresAt
            UserAccessState.FREE -> false
        }
    }
}

data class UserEntitlements(
    val invisibilityMode: Boolean,
    val dohEnabled: Boolean,
    val advancedStats: Boolean,
    val unlimitedWhitelist: Boolean,
    val customDns: Boolean,
    val showAds: Boolean
)

object AccessControl {
    fun entitlementsFor(state: UserAccessState): UserEntitlements {
        return when (state) {
            UserAccessState.TRIAL -> UserEntitlements(
                invisibilityMode = true,
                dohEnabled = true,
                advancedStats = true,
                unlimitedWhitelist = true,
                customDns = true,
                showAds = false // Clean Trial
            )

            UserAccessState.PREMIUM -> UserEntitlements(
                invisibilityMode = true,
                dohEnabled = true,
                advancedStats = true,
                unlimitedWhitelist = true,
                customDns = true,
                showAds = false
            )

            UserAccessState.FREE -> UserEntitlements(
                invisibilityMode = false,
                dohEnabled = false,
                advancedStats = false,
                unlimitedWhitelist = false,
                customDns = false,
                showAds = true
            )
        }
    }
}
