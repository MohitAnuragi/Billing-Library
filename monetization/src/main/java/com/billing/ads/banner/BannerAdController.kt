package com.billing.ads.banner

import android.content.Context
import android.view.ViewGroup
import com.billing.ads.banner.view.BannerSize
import com.billing.ads.meta.MetaProvider
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

    // The banner that is currently inside the container (only one of the two is set)
    private var adMobView: AdView? = null
    private var metaView: com.facebook.ads.AdView? = null

    @JvmOverloads
    fun loadAdMobBanner(adUnitId: String, size: BannerSize = BannerSize.STANDARD) {
        Logger.d("Loading AdMob Banner...")
        destroy()

        val adView = AdView(context)
        adView.setAdSize(adMobSize(size))
        adView.adUnitId = adUnitId

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() { Logger.d("AdMob Banner Loaded") }
            override fun onAdFailedToLoad(error: LoadAdError) {
                Logger.e("AdMob Banner Failed: ${error.message}")
            }
        }

        container.addView(adView)
        adMobView = adView
        adView.loadAd(AdRequest.Builder().build())
    }

    @JvmOverloads
    fun loadMetaBanner(adUnitId: String, size: BannerSize = BannerSize.STANDARD) {
        Logger.d("Loading Meta Banner...")
        destroy()
        MetaProvider.ensureInitialized(context)

        val adView = com.facebook.ads.AdView(context, adUnitId, metaSize(size))

        val config = adView.buildLoadAdConfig()
            .withAdListener(object : com.facebook.ads.AdListener {
                override fun onError(ad: com.facebook.ads.Ad?, error: com.facebook.ads.AdError?) {
                    Logger.e("Meta Banner Failed: ${error?.errorMessage}")
                }
                override fun onAdLoaded(ad: com.facebook.ads.Ad?) { Logger.d("Meta Banner Loaded") }
                override fun onAdClicked(ad: com.facebook.ads.Ad?) {}
                override fun onLoggingImpression(ad: com.facebook.ads.Ad?) {}
            }).build()

        container.addView(adView)
        metaView = adView
        adView.loadAd(config)
    }

    // Frees the current banner. Call when the screen goes away.
    fun destroy() {
        adMobView?.destroy()
        adMobView = null
        metaView?.destroy()
        metaView = null
        container.removeAllViews()
    }

    fun pause() {
        adMobView?.pause()
    }

    fun resume() {
        adMobView?.resume()
    }

    private fun adMobSize(size: BannerSize): AdSize = when (size) {
        BannerSize.STANDARD -> AdSize.BANNER
        BannerSize.ADAPTIVE -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adaptiveWidthDp())
    }

    // Meta has no adaptive size: its banners already fill the width, only the height changes
    private fun metaSize(size: BannerSize): com.facebook.ads.AdSize {
        val isTablet = context.resources.configuration.smallestScreenWidthDp >= 600
        return if (size == BannerSize.ADAPTIVE && isTablet) {
            com.facebook.ads.AdSize.BANNER_HEIGHT_90
        } else {
            com.facebook.ads.AdSize.BANNER_HEIGHT_50
        }
    }

    // Width of the container, or of the screen when the container is not laid out yet
    private fun adaptiveWidthDp(): Int {
        val metrics = context.resources.displayMetrics
        val widthPx = if (container.width > 0) container.width else metrics.widthPixels
        return (widthPx / metrics.density).toInt()
    }
}
