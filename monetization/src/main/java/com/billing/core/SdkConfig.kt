package com.billing.core


data class SdkConfig(
    val adMobAppId: String = "",
    val metaAppId: String = "",
    val isDebug: Boolean = false,
    val autoInitialize: Boolean = true,
    // When true, the next interstitial / rewarded ad is loaded automatically after one closes
    val autoReload: Boolean = true,
    // When true, ads are not initialized or loaded until ConsentManager says ads can be requested
    val requireConsent: Boolean = false,
    // Hashed device IDs printed in Logcat by the AdMob / UMP SDKs
    val testDeviceIds: List<String> = emptyList(),
    val metaTestDeviceIds: List<String> = emptyList(),
    // null = not specified
    val tagForChildDirectedTreatment: Boolean? = null,
    val tagForUnderAgeOfConsent: Boolean? = null
)
