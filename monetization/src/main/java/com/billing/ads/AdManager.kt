package com.billing.ads

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import com.billing.ads.admob.AdMobProvider
import com.billing.ads.meta.MetaProvider
import com.billing.consent.ConsentManager
import com.billing.core.MonetizationKit
import com.billing.core.interfaces.IAdProvider
import com.billing.utils.Logger

object AdManager {

    private const val ADMOB_APP_ID_KEY = "com.google.android.gms.ads.APPLICATION_ID"

    // "internal var" so tests can replace the real networks with fakes
    internal var adMobProvider: IAdProvider = AdMobProvider()
    internal var metaProvider: IAdProvider = MetaProvider()

    private var interstitialLoader = WaterfallLoader { isInterstitialReady() }
    private var rewardedLoader = WaterfallLoader { isRewardedReady() }

    // The last IDs the app asked for, kept so the next ad can be loaded automatically
    private var interstitialIds: Pair<String, String>? = null
    private var rewardedIds: Pair<String, String>? = null

    @SuppressLint("StaticFieldLeak")
    private var appContext: Context? = null

    private var isInitialized = false

    fun initialize(context: Context) {
        if (isInitialized) return

        MonetizationKit.checkInitialized("AdManager.initialize")
        if (!adsAllowed(context)) {
            Logger.e("AdManager.initialize skipped: consent has not been gathered")
            return
        }

        val config = MonetizationKit.config
        val appContext = context.applicationContext

        // AdMob reads its App ID from the manifest, so that is what decides if it can start
        val manifestAppId = readAdMobAppId(appContext)
        if (manifestAppId != null) {
            if (config.adMobAppId.isNotEmpty() && config.adMobAppId != manifestAppId) {
                Logger.e(
                    "AdMob App ID mismatch: SdkConfig has ${config.adMobAppId} but AndroidManifest has " +
                        "$manifestAppId. The manifest value is the one that is used."
                )
            }
            adMobProvider.initialize(appContext, manifestAppId)
        } else if (config.adMobAppId.isNotEmpty()) {
            Logger.e("AdMob not started: add the $ADMOB_APP_ID_KEY <meta-data> tag to your AndroidManifest")
        }

        // Without a metaAppId, Meta is started later, the first time a Meta ad is loaded
        if (config.metaAppId.isNotEmpty()) metaProvider.initialize(appContext, config.metaAppId)
        isInitialized = true
    }

    // Tries AdMob first and asks Meta only if AdMob has no ad.
    // onLoaded runs once an ad is ready; onFailed runs when every requested network failed.
    @JvmOverloads
    fun loadInterstitial(
        context: Context,
        adMobId: String,
        metaId: String,
        onLoaded: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        if (adMobId.isNotEmpty() || metaId.isNotEmpty()) interstitialIds = adMobId to metaId
        load(context, interstitialLoader, adMobId, metaId, onLoaded, onFailed) { provider, appContext, adUnitId, loaded, failed ->
            provider.loadInterstitial(appContext, adUnitId, loaded, failed)
        }
    }

    fun showInterstitial(activity: Activity, onClosed: () -> Unit) {
        MonetizationKit.checkInitialized("AdManager.showInterstitial")
        val closed = {
            onClosed()
            reload(interstitialIds, ::loadInterstitial)
        }

        if (adMobProvider.isInterstitialReady()) {
            adMobProvider.showInterstitial(activity, closed)
        } else if (metaProvider.isInterstitialReady()) {
            metaProvider.showInterstitial(activity, closed)
        } else {
            closed()
        }
    }

    fun isInterstitialReady(): Boolean =
        adMobProvider.isInterstitialReady() || metaProvider.isInterstitialReady()

    @JvmOverloads
    fun loadRewarded(
        context: Context,
        adMobId: String,
        metaId: String,
        onLoaded: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        if (adMobId.isNotEmpty() || metaId.isNotEmpty()) rewardedIds = adMobId to metaId
        load(context, rewardedLoader, adMobId, metaId, onLoaded, onFailed) { provider, appContext, adUnitId, loaded, failed ->
            provider.loadRewarded(appContext, adUnitId, loaded, failed)
        }
    }

    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit) {
        MonetizationKit.checkInitialized("AdManager.showRewarded")
        val closed = {
            onAdClosed()
            reload(rewardedIds, ::loadRewarded)
        }

        // Simple priority: AdMob -> Meta
        if (adMobProvider.isRewardedReady()) {
            adMobProvider.showRewarded(activity, onRewardEarned, closed)
        } else if (metaProvider.isRewardedReady()) {
            metaProvider.showRewarded(activity, onRewardEarned, closed)
        } else {
            closed()
        }
    }

    fun isRewardedReady(): Boolean =
        adMobProvider.isRewardedReady() || metaProvider.isRewardedReady()

    internal fun adsAllowed(context: Context): Boolean =
        !MonetizationKit.config.requireConsent || ConsentManager.canRequestAds(context)

    private fun load(
        context: Context,
        loader: WaterfallLoader,
        adMobId: String,
        metaId: String,
        onLoaded: () -> Unit,
        onFailed: (String) -> Unit,
        request: (IAdProvider, Context, String, () -> Unit, (String) -> Unit) -> Unit
    ) {
        MonetizationKit.checkInitialized("AdManager load")
        if (!adsAllowed(context)) {
            Logger.e("Ad load skipped: consent has not been gathered")
            onFailed("Consent has not been gathered")
            return
        }

        // App Context so the singleton never holds on to an Activity
        val appContext = context.applicationContext
        this.appContext = appContext

        val steps = mutableListOf<LoadStep>()
        if (adMobId.isNotEmpty()) {
            steps.add { loaded, failed -> request(adMobProvider, appContext, adMobId, loaded, failed) }
        }
        if (metaId.isNotEmpty()) {
            steps.add { loaded, failed -> request(metaProvider, appContext, metaId, loaded, failed) }
        }
        loader.load(steps, onLoaded, onFailed)
    }

    // Loads the next ad after one was shown (or when none was ready), using the app's last IDs.
    // Safe to combine with a manual reload in the app's own callback: the loader ignores duplicates.
    private fun reload(
        ids: Pair<String, String>?,
        loadAd: (Context, String, String, () -> Unit, (String) -> Unit) -> Unit
    ) {
        val context = appContext ?: return
        if (ids == null || !MonetizationKit.config.autoReload) return
        loadAd(context, ids.first, ids.second, {}, {})
    }

    internal fun resetForTest() {
        adMobProvider = AdMobProvider()
        metaProvider = MetaProvider()
        interstitialLoader = WaterfallLoader { isInterstitialReady() }
        rewardedLoader = WaterfallLoader { isRewardedReady() }
        interstitialIds = null
        rewardedIds = null
        appContext = null
        isInitialized = false
    }

    private fun readAdMobAppId(context: Context): String? = try {
        context.packageManager
            .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            .metaData?.getString(ADMOB_APP_ID_KEY)
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}
