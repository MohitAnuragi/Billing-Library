package com.billing.ads.admob

import android.app.Activity
import android.content.Context
import com.billing.core.interfaces.IAdProvider
import com.billing.utils.Logger
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class AdMobProvider : IAdProvider {

    private var mInterstitialAd: InterstitialAd? = null
    private var mRewardedAd: RewardedAd? = null

    override fun initialize(context: Context, appId: String) {
        MobileAds.initialize(context) { status ->
            Logger.d("AdMob Init: ${status.adapterStatusMap}")
        }
    }

    override fun loadInterstitial(context: Context, adUnitId: String) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(context, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                Logger.d("AdMob Interstitial Loaded")
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                mInterstitialAd = null
                Logger.e("AdMob Interstitial Failed: ${adError.message}")
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
                    mInterstitialAd = null
                    onAdClosed()
                }
            }
            mInterstitialAd?.show(activity)
        } else {
            onAdClosed()
        }
    }

    override fun loadRewarded(context: Context, adUnitId: String) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(context, adUnitId, adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(rewardedAd: RewardedAd) {
                mRewardedAd = rewardedAd
                Logger.d("AdMob Rewarded Loaded")
            }
            override fun onAdFailedToLoad(error: LoadAdError) {
                mRewardedAd = null
                Logger.e("AdMob Rewarded Failed: ${error.message}")
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
            }
            mRewardedAd?.show(activity) { _ -> onRewardEarned() }
        } else {
            onAdClosed()
        }
    }

    override fun isInterstitialReady(): Boolean = mInterstitialAd != null
}