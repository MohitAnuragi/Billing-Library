package com.billing.billinglibrary


import android.app.Application
import com.billing.core.MonetizationKit
import com.billing.core.SdkConfig


class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Create Config (Using Test IDs)
        val config = SdkConfig(
            adMobAppId = "ca-app-pub-3940256099942544~3347511713",
            metaAppId = "YOUR_META_APP_ID",
            isDebug = true,
            autoInitialize = true
        )


        // 2. Initialize Library
        MonetizationKit.init(this, config)
    }
}