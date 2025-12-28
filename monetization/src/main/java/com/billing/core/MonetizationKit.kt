package com.billing.core


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.billing.review.InAppReviewManager
import com.billing.update.InAppUpdateManager

object MonetizationKit {

    private const val TAG = "MonetizationKit"

    // Internal access to config so other modules (Ads/Play) can read it
    internal var config: SdkConfig = SdkConfig()
        private set

    @SuppressLint("StaticFieldLeak")
    internal var appContext: Context? = null
        private set

    private var isInitialized = false


    fun init(context: Context, config: SdkConfig) {
        if (isInitialized) {
            Log.w(TAG, "MonetizationKit is already initialized.")
            return
        }

        this.config = config
        this.appContext = context.applicationContext // Use App Context to prevent leaks
        this.isInitialized = true

        if (config.isDebug) {
            Log.d(TAG, "MonetizationKit initialized in DEBUG mode.")
        }

        if (config.autoInitialize) {
            // AdManager.initialize(context) -> We will uncomment this when we build the 'ads' package
        }
    }
    fun requestAppReview(activity: Activity) {
        InAppReviewManager(activity).requestReview()
    }

    fun checkForAppUpdate(activity: Activity, isFlexible: Boolean = true) {
        InAppUpdateManager(activity).checkForUpdate(isFlexible)
    }

}