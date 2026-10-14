package com.billing.core


import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.util.Log
import com.billing.ads.AdManager
import com.billing.review.InAppReviewManager
import com.billing.update.InAppUpdateManager
import com.billing.update.UpdateState
import com.billing.update.UpdateStatus

object MonetizationKit {

    private const val TAG = "MonetizationKit"

    // Internal access to config so other modules (Ads/Play) can read it
    internal var config: SdkConfig = SdkConfig()
        private set

    @SuppressLint("StaticFieldLeak")
    internal var appContext: Context? = null
        private set

    internal var isInitialized = false
        private set

    private var hasWarnedNotInitialized = false


    fun init(context: Context, config: SdkConfig) {
        if (isInitialized) {
            Log.w(TAG, "MonetizationKit is already initialized.")
            return
        }

        this.config = config
        this.appContext = context.applicationContext // Use App Context to prevent leaks
        this.isInitialized = true

        // Registered here so app foreground / background is tracked from the very start
        UpdateState.register(context)

        if (config.isDebug) {
            Log.d(TAG, "MonetizationKit initialized in DEBUG mode.")
        }

        // With requireConsent, this only runs for users who already consented;
        // new users are initialized by ConsentManager.gatherConsent
        if (config.autoInitialize && AdManager.adsAllowed(context)) {
            AdManager.initialize(context)
        }
    }

    // onComplete(true) means the review flow finished without an error.
    // Google never tells the app whether the rating dialog was actually shown.
    @JvmOverloads
    fun requestAppReview(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
        InAppReviewManager(activity).requestReview(onComplete)
    }

    // onDownloaded: called when a flexible update is downloaded; show your own "Restart" prompt
    // and call completeAppUpdate(). When null, the update is installed the next time the app
    // goes to the background.
    @JvmOverloads
    fun checkForAppUpdate(
        activity: Activity,
        isFlexible: Boolean = true,
        onDownloaded: (() -> Unit)? = null,
        onStatus: (UpdateStatus) -> Unit = {}
    ) {
        InAppUpdateManager(activity).checkForUpdate(isFlexible, onDownloaded, onStatus)
    }

    // Installs a downloaded flexible update. The app restarts.
    fun completeAppUpdate() {
        UpdateState.completeUpdate()
    }

    internal fun resetForTest() {
        config = SdkConfig()
        appContext = null
        isInitialized = false
        hasWarnedNotInitialized = false
    }

    // Logs once when the library is used before init(), instead of failing silently
    internal fun checkInitialized(caller: String) {
        if (isInitialized || hasWarnedNotInitialized) return
        hasWarnedNotInitialized = true
        Log.e(TAG, "$caller was used before MonetizationKit.init(). Call init() in Application.onCreate().")
    }

}