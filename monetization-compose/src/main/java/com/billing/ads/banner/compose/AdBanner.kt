package com.billing.ads.banner.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.billing.ads.banner.view.AdBannerView
import com.billing.ads.banner.view.BannerSize
import com.billing.ads.banner.view.BannerType

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String,
    type: BannerType = BannerType.ADMOB,
    size: BannerSize = BannerSize.STANDARD
) {
    // A new banner is created whenever the ad unit, network or size changes
    key(adUnitId, type, size) {
        AndroidView(
            modifier = modifier,
            factory = { context ->
                AdBannerView(context).apply {
                    loadAd(adUnitId, type, size)
                }
            },
            onRelease = { it.destroy() }
        )
    }
}
