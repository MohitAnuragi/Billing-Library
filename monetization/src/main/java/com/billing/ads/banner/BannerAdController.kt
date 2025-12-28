package com.billing.ads.banner

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import androidx.annotation.RequiresPermission
import com.billing.utils.Logger
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

class BannerAdController(
    private val context: Context,
    private val container: ViewGroup
) {

    @RequiresPermission(Manifest.permission.INTERNET)
    fun loadAdMobBanner(adUnitId: String) {
        Logger.d("Loading AdMob Banner...")
        val adView = AdView(context)
        adView.setAdSize(AdSize.BANNER)
        adView.adUnitId = adUnitId

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() { Logger.d("AdMob Banner Loaded") }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Logger.e("AdMob Banner Failed: ${error.message}")
            }
        }

        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(AdRequest.Builder().build())
    }

    fun loadMetaBanner(adUnitId: String) {
        Logger.d("Loading Meta Banner...")
        val adView = com.facebook.ads.AdView(
            context,
            adUnitId,
            com.facebook.ads.AdSize.BANNER_HEIGHT_50
        )

        val config = adView.buildLoadAdConfig()
            .withAdListener(object : com.facebook.ads.AdListener {
                override fun onError(ad: com.facebook.ads.Ad?, error: com.facebook.ads.AdError?) {
                    Logger.e("Meta Banner Failed: ${error?.errorMessage}")
                }
                override fun onAdLoaded(ad: com.facebook.ads.Ad?) { Logger.d("Meta Banner Loaded") }
                override fun onAdClicked(ad: com.facebook.ads.Ad?) {}
                override fun onLoggingImpression(ad: com.facebook.ads.Ad?) {}
            }).build()

        container.removeAllViews()
        container.addView(adView)
        adView.loadAd(config)
    }
}