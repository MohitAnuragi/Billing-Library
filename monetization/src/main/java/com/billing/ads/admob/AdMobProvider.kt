package com.billing.ads.admob

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import com.billing.core.MonetizationKit
import com.billing.core.interfaces.IAdProvider
import com.billing.utils.Logger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobProvider : IAdProvider {

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    private var interstitialLoadedAt = 0L
    private var rewardedLoadedAt = 0L

    override fun initialize(context: Context, appId: String) {
        applyRequestConfiguration()
        MobileAds.initialize(context) { status ->
            Logger.d("AdMob Init: ${status.adapterStatusMap}")
        }
    }

    private fun applyRequestConfiguration() {
        val config = MonetizationKit.config
        val builder = MobileAds.getRequestConfiguration().toBuilder()

        if (config.testDeviceIds.isNotEmpty()) {
            builder.setTestDeviceIds(config.testDeviceIds)
        }
        config.tagForChildDirectedTreatment?.let { isChildDirected ->
            builder.setTagForChildDirectedTreatment(
                if (isChildDirected) RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE
                else RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE
            )
        }
        config.tagForUnderAgeOfConsent?.let { isUnderAge ->
            builder.setTagForUnderAgeOfConsent(
                if (isUnderAge) RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_TRUE
                else RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE
            )
        }

        MobileAds.setRequestConfiguration(builder.build())
    }

    override fun loadInterstitial(
        context: Context,
        adUnitId: String,
        onLoaded: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        val adRequest = AdRequest.Builder().build()
        // App Context so the singleton never holds on to an Activity
        InterstitialAd.load(context.applicationContext, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                interstitialLoadedAt = SystemClock.elapsedRealtime()
                Logger.d("AdMob Interstitial Loaded")
                onLoaded()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
                Logger.e("AdMob Interstitial Failed: ${adError.message}")
                onFailed(adError.message)
            }
        })
    }

    override fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    onAdClosed()
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    Logger.e("AdMob Interstitial Failed To Show: ${p0.message}")
                    mInterstitialAd = null
                    onAdClosed()
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            onAdClosed()
        }
    }

    override fun loadRewarded(
        context: Context,
        adUnitId: String,
        onLoaded: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context.applicationContext, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
                rewardedLoadedAt = SystemClock.elapsedRealtime()
                Logger.d("AdMob Rewarded Loaded")
                onLoaded()
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                mRewardedAd = null
                Logger.e("AdMob Rewarded Failed: ${error.message}")
                onFailed(error.message)
            }
        })
    }

    override fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        if (mRewardedAd != null) {
            mRewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mRewardedAd = null
                    onAdClosed()
                }
                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    Logger.e("AdMob Rewarded Failed To Show: ${p0.message}")
                    mRewardedAd = null
                    onAdClosed()
                }
            }
            mRewardedAd?.show(activity) { _ -> onRewardEarned() }
        } else {
            onAdClosed()
        }
    }

    override fun isInterstitialReady(): Boolean =
        mInterstitialAd != null && !isExpired(interstitialLoadedAt)

    override fun isRewardedReady(): Boolean =
        mRewardedAd != null && !isExpired(rewardedLoadedAt)

    private fun isExpired(loadedAt: Long): Boolean =
        SystemClock.elapsedRealtime() - loadedAt >= AD_LIFETIME_MS

    private companion object {
        // AdMob stops serving a loaded ad after one hour
        const val AD_LIFETIME_MS = 60 * 60 * 1000L
    }
}
