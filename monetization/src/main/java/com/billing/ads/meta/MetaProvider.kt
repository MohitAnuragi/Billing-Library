package com.billing.ads.meta

import android.app.Activity
import android.content.Context
import com.billing.core.MonetizationKit
import com.billing.core.interfaces.IAdProvider
import com.billing.utils.Logger
import com.facebook.ads.*

class MetaProvider : IAdProvider {

    private var metaInterstitial: InterstitialAd? = null
    private var metaRewarded: RewardedVideoAd? = null

    // The ad that is on screen right now (null when nothing is showing)
    private var showingInterstitial: InterstitialAd? = null
    private var showingRewarded: RewardedVideoAd? = null

    private var onInterstitialClosed: (() -> Unit)? = null
    private var onRewardedClosed: (() -> Unit)? = null
    private var onUserEarnedReward: (() -> Unit)? = null

    override fun initialize(context: Context, appId: String) {
        ensureInitialized(context)
    }

    override fun loadInterstitial(
        context: Context,
        adUnitId: String,
        onLoaded: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        ensureInitialized(context)

        // An ad that is on screen is destroyed by its own listener when it closes
        if (metaInterstitial !== showingInterstitial) metaInterstitial?.destroy()

        // App Context so the singleton never holds on to an Activity
        val interstitial = InterstitialAd(context.applicationContext, adUnitId)
        this.metaInterstitial = interstitial

        val config = interstitial.buildLoadAdConfig()
            .withAdListener(object : InterstitialAdListener {
                override fun onInterstitialDismissed(ad: Ad?) {
                    releaseInterstitial(interstitial)
                    finishInterstitialShow()
                }

                override fun onError(ad: Ad?, error: AdError?) {
                    val message = error?.errorMessage ?: "Unknown error"
                    Logger.e("Meta Interstitial Error: $message")
                    val wasShowing = showingInterstitial === interstitial
                    releaseInterstitial(interstitial)
                    if (wasShowing) finishInterstitialShow() else onFailed(message)
                }

                override fun onAdLoaded(ad: Ad?) {
                    Logger.d("Meta Interstitial Loaded")
                    onLoaded()
                }

                override fun onAdClicked(ad: Ad?) {}
                override fun onLoggingImpression(ad: Ad?) {}
                override fun onInterstitialDisplayed(ad: Ad?) {}
            })
            .build()

        interstitial.loadAd(config)
    }

    override fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        val ad = metaInterstitial
        if (ad != null && ad.isAdLoaded && !ad.isAdInvalidated) {
            // 1. Update the callback variable so the Listener (defined above) can use it
            this.onInterstitialClosed = onAdClosed
            this.showingInterstitial = ad

            // 2. Build simple show config
            val showConfig = ad.buildShowAdConfig().build()
            if (!ad.show(showConfig)) {
                Logger.e("Meta Interstitial Failed To Show")
                releaseInterstitial(ad)
                finishInterstitialShow()
            }
        } else {
            onAdClosed()
        }
    }

    private fun releaseInterstitial(ad: InterstitialAd) {
        ad.destroy()
        if (metaInterstitial === ad) metaInterstitial = null
    }

    // Runs the saved close callback once and drops it, so no Activity stays referenced
    private fun finishInterstitialShow() {
        val closed = onInterstitialClosed
        onInterstitialClosed = null
        showingInterstitial = null
        closed?.invoke()
    }

    override fun loadRewarded(
        context: Context,
        adUnitId: String,
        onLoaded: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        ensureInitialized(context)

        if (metaRewarded !== showingRewarded) metaRewarded?.destroy()

        val rewarded = RewardedVideoAd(context.applicationContext, adUnitId)
        this.metaRewarded = rewarded

        // Attach listener during Load
        val config = rewarded.buildLoadAdConfig()
            .withAdListener(object : RewardedVideoAdListener {
                override fun onRewardedVideoClosed() {
                    releaseRewarded(rewarded)
                    finishRewardedShow()
                }

                override fun onRewardedVideoCompleted() {
                    // This triggers when user finishes video
                    val earned = onUserEarnedReward
                    onUserEarnedReward = null
                    earned?.invoke()
                }

                override fun onError(ad: Ad?, error: AdError?) {
                    val message = error?.errorMessage ?: "Unknown error"
                    Logger.e("Meta Rewarded Error: $message")
                    val wasShowing = showingRewarded === rewarded
                    releaseRewarded(rewarded)
                    if (wasShowing) finishRewardedShow() else onFailed(message)
                }

                override fun onAdLoaded(ad: Ad?) {
                    Logger.d("Meta Rewarded Loaded")
                    onLoaded()
                }

                override fun onAdClicked(ad: Ad?) {}
                override fun onLoggingImpression(ad: Ad?) {}
            })
            .build()

        rewarded.loadAd(config)
    }

    override fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        val ad = metaRewarded
        if (ad != null && ad.isAdLoaded && !ad.isAdInvalidated) {
            // 1. Update callback variables
            this.onUserEarnedReward = onRewardEarned
            this.onRewardedClosed = onAdClosed
            this.showingRewarded = ad

            // 2. Build simple show config (No listener here)
            val showConfig = ad.buildShowAdConfig().build()
            if (!ad.show(showConfig)) {
                Logger.e("Meta Rewarded Failed To Show")
                releaseRewarded(ad)
                finishRewardedShow()
            }
        } else {
            onAdClosed()
        }
    }

    private fun releaseRewarded(ad: RewardedVideoAd) {
        ad.destroy()
        if (metaRewarded === ad) metaRewarded = null
    }

    private fun finishRewardedShow() {
        val closed = onRewardedClosed
        onRewardedClosed = null
        onUserEarnedReward = null
        showingRewarded = null
        closed?.invoke()
    }

    override fun isInterstitialReady(): Boolean =
        metaInterstitial?.let { it.isAdLoaded && !it.isAdInvalidated } == true

    override fun isRewardedReady(): Boolean =
        metaRewarded?.let { it.isAdLoaded && !it.isAdInvalidated } == true

    internal companion object {
        // Starts the Meta SDK the first time it is needed, so Meta ads also work
        // when metaAppId was left empty in SdkConfig
        fun ensureInitialized(context: Context) {
            val appContext = context.applicationContext
            if (AudienceNetworkAds.isInitialized(appContext)) return

            val config = MonetizationKit.config
            if (config.metaTestDeviceIds.isNotEmpty()) {
                AdSettings.addTestDevices(config.metaTestDeviceIds)
            }
            if (config.tagForChildDirectedTreatment == true) {
                AdSettings.setMixedAudience(true)
            }
            AudienceNetworkAds.initialize(appContext)
        }
    }
}
