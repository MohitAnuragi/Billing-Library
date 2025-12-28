package com.billing.core


data class SdkConfig(
    val adMobAppId: String = "",
    val metaAppId: String = "",
    val isDebug: Boolean = false,
    val autoInitialize: Boolean = true
)