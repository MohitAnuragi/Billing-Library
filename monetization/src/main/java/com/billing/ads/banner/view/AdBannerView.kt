package com.billing.ads.banner.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import com.billing.ads.AdManager
import com.billing.ads.banner.BannerAdController
import com.billing.core.MonetizationKit
import com.billing.utils.Logger

enum class BannerType { ADMOB, META }

// STANDARD = fixed 320x50. ADAPTIVE = full width, height chosen for the device.
enum class BannerSize { STANDARD, ADAPTIVE }

class AdBannerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private class Request(val adUnitId: String, val type: BannerType, val size: BannerSize)

    private val controller = BannerAdController(context, this)

    private var lastRequest: Request? = null
    private var isDestroyedByDetach = false

    @JvmOverloads
    fun loadAd(adUnitId: String, type: BannerType, size: BannerSize = BannerSize.STANDARD) {
        MonetizationKit.checkInitialized("AdBannerView.loadAd")
        if (!AdManager.adsAllowed(context)) {
            Logger.e("Banner load skipped: consent has not been gathered")
            return
        }

        lastRequest = Request(adUnitId, type, size)
        isDestroyedByDetach = false
        when (type) {
            BannerType.ADMOB -> controller.loadAdMobBanner(adUnitId, size)
            BannerType.META -> controller.loadMetaBanner(adUnitId, size)
        }
    }

    // Frees the banner for good. Call loadAd again to show a new one.
    fun destroy() {
        lastRequest = null
        isDestroyedByDetach = false
        controller.destroy()
    }

    // The banner is freed when the view leaves the screen...
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (lastRequest != null) {
            controller.destroy()
            isDestroyedByDetach = true
        }
    }

    // ...and loaded again if the same view comes back (RecyclerView, Fragment back stack)
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val request = lastRequest
        if (isDestroyedByDetach && request != null) {
            loadAd(request.adUnitId, request.type, request.size)
        }
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE) controller.resume() else controller.pause()
    }
}
