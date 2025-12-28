package com.billing.ads.banner.view

import android.Manifest
import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.RequiresPermission
import com.billing.ads.banner.BannerAdController

enum class BannerType { ADMOB, META }

class AdBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val controller = BannerAdController(context, this)

    @RequiresPermission(Manifest.permission.INTERNET)
    fun loadAd(adUnitId: String, type: BannerType) {
        when (type) {
            BannerType.ADMOB -> controller.loadAdMobBanner(adUnitId)
            BannerType.META -> controller.loadMetaBanner(adUnitId)
        }
    }
}