package com.billing.consent

import android.app.Activity
import android.content.Context
import com.billing.ads.AdManager
import com.billing.core.MonetizationKit
import com.billing.utils.Logger
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {

    // Asks Google's UMP SDK for the user's consent status and shows the consent form if one is needed.
    // onComplete always runs, with true when ads may be requested.
    fun gatherConsent(activity: Activity, onComplete: (canRequestAds: Boolean) -> Unit) {
        MonetizationKit.checkInitialized("ConsentManager.gatherConsent")
        val config = MonetizationKit.config
        val appContext = activity.applicationContext
        val consentInformation = UserMessagingPlatform.getConsentInformation(appContext)

        val params = ConsentRequestParameters.Builder()
        config.tagForUnderAgeOfConsent?.let { params.setTagForUnderAgeOfConsent(it) }

        // Test devices in debug builds are treated as being in the EEA, so the form can be tested anywhere
        if (config.isDebug && config.testDeviceIds.isNotEmpty()) {
            val debugSettings = ConsentDebugSettings.Builder(appContext)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            config.testDeviceIds.forEach { debugSettings.addTestDeviceHashedId(it) }
            params.setConsentDebugSettings(debugSettings.build())
        }

        val finish = {
            // Consent from an earlier session still counts when this session's request failed
            val canRequestAds = consentInformation.canRequestAds()
            if (canRequestAds && config.autoInitialize) AdManager.initialize(appContext)
            onComplete(canRequestAds)
        }

        consentInformation.requestConsentInfoUpdate(
            activity,
            params.build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) Logger.e("Consent Form Failed: ${formError.message}")
                    finish()
                }
            },
            { requestError ->
                Logger.e("Consent Update Failed: ${requestError.message}")
                finish()
            }
        )
    }

    fun canRequestAds(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context.applicationContext).canRequestAds()

    // True when the app must offer a "Privacy settings" entry that calls showPrivacyOptionsForm
    fun isPrivacyOptionsRequired(context: Context): Boolean =
        UserMessagingPlatform.getConsentInformation(context.applicationContext)
            .privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    @JvmOverloads
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) Logger.e("Privacy Options Form Failed: ${formError.message}")
            onDismissed()
        }
    }
}
