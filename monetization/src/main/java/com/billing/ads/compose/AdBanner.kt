package com.billing.ads.banner.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.billing.ads.banner.view.AdBannerView
import com.billing.ads.banner.view.BannerType

@Composable
fun AdBanner(
    modifier: Modifier = Modifier,
    adUnitId: String,
    type: BannerType = BannerType.ADMOB
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdBannerView(context).apply {
                loadAd(adUnitId, type)
            }
        }
    )
}