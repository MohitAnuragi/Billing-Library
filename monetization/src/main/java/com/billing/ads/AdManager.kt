package com.billing.ads

import android.app.Activity
import android.content.Context
import com.billing.ads.admob.AdMobProvider
import com.billing.ads.meta.MetaProvider
import com.billing.core.MonetizationKit

object AdManager {

    private val adMobProvider by lazy { AdMobProvider() }
    private val metaProvider by lazy { MetaProvider() }

    fun initialize(context: Context) {
        val config = MonetizationKit.config
        if (config.adMobAppId.isNotEmpty()) adMobProvider.initialize(context, config.adMobAppId)
        if (config.metaAppId.isNotEmpty()) metaProvider.initialize(context, config.metaAppId)
    }

    fun loadInterstitial(context: Context, adMobId: String, metaId: String) {
        if (adMobId.isNotEmpty()) adMobProvider.loadInterstitial(context, adMobId)
        if (metaId.isNotEmpty()) metaProvider.loadInterstitial(context, metaId)
    }

    fun showInterstitial(activity: Activity, onClosed: () -> Unit) {
        if (adMobProvider.isInterstitialReady()) {
            adMobProvider.showInterstitial(activity, onClosed)
        } else if (metaProvider.isInterstitialReady()) {
            metaProvider.showInterstitial(activity, onClosed)
        } else {
            onClosed()
        }
    }

    fun loadRewarded(context: Context, adMobId: String, metaId: String) {
        if (adMobId.isNotEmpty()) adMobProvider.loadRewarded(context, adMobId)
        if (metaId.isNotEmpty()) metaProvider.loadRewarded(context, metaId)
    }

    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        // Simple priority: AdMob -> Meta
        // Note: You can add an isRewardedReady() check if you update the interface,
        // for now, we try AdMob first blindly or based on internal state if exposed.
        // Ideally, update IAdProvider to have isRewardedReady() too.
        adMobProvider.showRewarded(activity, onRewardEarned, onAdClosed)
    }
}