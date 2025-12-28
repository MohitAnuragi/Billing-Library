package com.billing.ads.meta

import android.app.Activity
import android.content.Context
import com.billing.core.interfaces.IAdProvider
import com.billing.utils.Logger
import com.facebook.ads.*

class MetaProvider : IAdProvider {

    private var metaInterstitial: InterstitialAd? = null
    private var metaRewarded: RewardedVideoAd? = null

    private var onInterstitialClosed: (() -> Unit)? = null
    private var onRewardedClosed: (() -> Unit)? = null
    private var onUserEarnedReward: (() -> Unit)? = null

    override fun initialize(context: Context, appId: String) {
        if (!AudienceNetworkAds.isInitialized(context)) {
            AudienceNetworkAds.initialize(context)
        }
    }

    override fun loadInterstitial(context: Context, adUnitId: String) {
        val interstitial = InterstitialAd(context, adUnitId)
        this.metaInterstitial = interstitial

        val config = interstitial.buildLoadAdConfig()
            .withAdListener(object : InterstitialAdListener {
                override fun onInterstitialDismissed(ad: Ad?) {
                    metaInterstitial = null
                    onInterstitialClosed?.invoke()
                }

                override fun onError(ad: Ad?, error: AdError?) {
                    Logger.e("Meta Interstitial Error: ${error?.errorMessage}")
                    metaInterstitial = null
                }

                override fun onAdLoaded(ad: Ad?) {
                    Logger.d("Meta Interstitial Loaded")
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
        if (ad != null && ad.isAdLoaded) {
            // 1. Update the callback variable so the Listener (defined above) can use it
            this.onInterstitialClosed = onAdClosed

            // 2. Build simple show config
            val showConfig = ad.buildShowAdConfig().build()
            ad.show(showConfig)
        } else {
            onAdClosed()
        }
    }

    override fun loadRewarded(context: Context, adUnitId: String) {
        val rewarded = RewardedVideoAd(context, adUnitId)
        this.metaRewarded = rewarded

        // Attach listener during Load
        val config = rewarded.buildLoadAdConfig()
            .withAdListener(object : RewardedVideoAdListener {
                override fun onRewardedVideoClosed() {
                    metaRewarded = null
                    onRewardedClosed?.invoke()
                }

                override fun onRewardedVideoCompleted() {
                    // This triggers when user finishes video
                    onUserEarnedReward?.invoke()
                }

                override fun onError(ad: Ad?, error: AdError?) {
                    Logger.e("Meta Rewarded Error: ${error?.errorMessage}")
                    metaRewarded = null
                }

                override fun onAdLoaded(ad: Ad?) {
                    Logger.d("Meta Rewarded Loaded")
                }

                override fun onAdClicked(ad: Ad?) {}
                override fun onLoggingImpression(ad: Ad?) {}
            })
            .build()

        rewarded.loadAd(config)
    }

    override fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        val ad = metaRewarded
        if (ad != null && ad.isAdLoaded) {
            // 1. Update callback variables
            this.onUserEarnedReward = onRewardEarned
            this.onRewardedClosed = onAdClosed

            // 2. Build simple show config (No listener here)
            val showConfig = ad.buildShowAdConfig().build()
            ad.show(showConfig)
        } else {
            onAdClosed()
        }
    }

    override fun isInterstitialReady(): Boolean = metaInterstitial?.isAdLoaded == true
}