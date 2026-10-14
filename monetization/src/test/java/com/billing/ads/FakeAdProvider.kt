package com.billing.ads

import android.app.Activity
import android.content.Context
import com.billing.core.interfaces.IAdProvider

// A fake ad network. It never shows anything: the test decides when an ad loads, fails or closes.
class FakeAdProvider : IAdProvider {

    var initializeCalls = 0
    var interstitialRequests = 0
    var rewardedRequests = 0
    var interstitialsShown = 0
    var rewardedShown = 0

    private var isInterstitialLoaded = false
    private var isRewardedLoaded = false

    private var onInterstitialLoaded: () -> Unit = {}
    private var onInterstitialFailed: (String) -> Unit = {}
    private var onRewardedLoaded: () -> Unit = {}
    private var onRewardedFailed: (String) -> Unit = {}
    private var onInterstitialClosed: () -> Unit = {}
    private var onRewardEarned: () -> Unit = {}
    private var onRewardedClosed: () -> Unit = {}

    // --- What the test calls to play the network's part ---

    fun finishInterstitialLoad() {
        isInterstitialLoaded = true
        onInterstitialLoaded()
    }

    fun failInterstitialLoad(error: String = "No fill") = onInterstitialFailed(error)

    fun closeInterstitial() = onInterstitialClosed()

    fun finishRewardedLoad() {
        isRewardedLoaded = true
        onRewardedLoaded()
    }

    fun failRewardedLoad(error: String = "No fill") = onRewardedFailed(error)

    fun earnReward() = onRewardEarned()

    fun closeRewarded() = onRewardedClosed()

    // --- IAdProvider ---

    override fun initialize(context: Context, appId: String) {
        initializeCalls++
    }

    override fun loadInterstitial(
        context: Context,
        adUnitId: String,
        onLoaded: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        interstitialRequests++
        onInterstitialLoaded = onLoaded
        onInterstitialFailed = onFailed
    }

    override fun showInterstitial(activity: Activity, onAdClosed: () -> Unit) {
        interstitialsShown++
        isInterstitialLoaded = false
        onInterstitialClosed = onAdClosed
    }

    override fun loadRewarded(
        context: Context,
        adUnitId: String,
        onLoaded: () -> Unit,
        onFailed: (String) -> Unit
    ) {
        rewardedRequests++
        onRewardedLoaded = onLoaded
        onRewardedFailed = onFailed
    }

    override fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        rewardedShown++
        isRewardedLoaded = false
        this.onRewardEarned = onRewardEarned
        onRewardedClosed = onAdClosed
    }

    override fun isInterstitialReady(): Boolean = isInterstitialLoaded

    override fun isRewardedReady(): Boolean = isRewardedLoaded
}
