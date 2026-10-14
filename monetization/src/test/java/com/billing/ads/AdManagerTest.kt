package com.billing.ads

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.billing.core.MonetizationKit
import com.billing.core.SdkConfig
import com.billing.update.UpdateState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AdManagerTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private val adMob = FakeAdProvider()
    private val meta = FakeAdProvider()

    private var loadedCount = 0
    private val failures = mutableListOf<String>()
    private var closedCount = 0

    @Before
    fun setUp() {
        MonetizationKit.resetForTest()
        UpdateState.resetForTest()
        AdManager.resetForTest()
        AdManager.adMobProvider = adMob
        AdManager.metaProvider = meta
    }

    private fun init(config: SdkConfig = SdkConfig(autoInitialize = false)) =
        MonetizationKit.init(context, config)

    private fun loadInterstitial(adMobId: String = "admob-id", metaId: String = "meta-id") =
        AdManager.loadInterstitial(context, adMobId, metaId, { loadedCount++ }, { failures.add(it) })

    private fun activity(): Activity = Robolectric.buildActivity(Activity::class.java).setup().get()

    // ---------- Loading ----------

    @Test
    fun adMobLoads_metaIsNeverAsked() {
        init()
        loadInterstitial()
        adMob.finishInterstitialLoad()

        assertEquals(1, adMob.interstitialRequests)
        assertEquals(0, meta.interstitialRequests)
        assertEquals(1, loadedCount)
        assertTrue(AdManager.isInterstitialReady())
    }

    @Test
    fun adMobFails_metaIsAskedAndLoads() {
        init()
        loadInterstitial()
        adMob.failInterstitialLoad()
        meta.finishInterstitialLoad()

        assertEquals(1, meta.interstitialRequests)
        assertEquals(1, loadedCount)
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun bothNetworksFail_onFailedGetsTheLastError() {
        init()
        loadInterstitial()
        adMob.failInterstitialLoad("AdMob no fill")
        meta.failInterstitialLoad("Meta no fill")

        assertEquals(0, loadedCount)
        assertEquals(listOf("Meta no fill"), failures)
        assertFalse(AdManager.isInterstitialReady())
    }

    @Test
    fun emptyMetaId_onlyAdMobIsAsked() {
        init()
        loadInterstitial(metaId = "")
        adMob.failInterstitialLoad("AdMob no fill")

        assertEquals(0, meta.interstitialRequests)
        assertEquals(listOf("AdMob no fill"), failures)
    }

    @Test
    fun secondLoadWhileLoading_sendsOneRequestAndNotifiesBothCallers() {
        init()
        loadInterstitial()
        loadInterstitial()
        adMob.finishInterstitialLoad()

        assertEquals(1, adMob.interstitialRequests)
        assertEquals(2, loadedCount)
    }

    @Test
    fun loadWhenAnAdIsReady_sendsNoRequest() {
        init()
        loadInterstitial()
        adMob.finishInterstitialLoad()
        loadInterstitial()

        assertEquals(1, adMob.interstitialRequests)
        assertEquals(2, loadedCount)
    }

    @Test
    fun consentRequiredButNotGathered_loadFailsWithoutARequest() {
        init(SdkConfig(autoInitialize = false, requireConsent = true))
        loadInterstitial()

        assertEquals(0, adMob.interstitialRequests)
        assertEquals(0, meta.interstitialRequests)
        assertEquals(listOf("Consent has not been gathered"), failures)
    }

    // ---------- Showing ----------

    @Test
    fun show_prefersAdMob() {
        init()
        loadInterstitial()
        adMob.finishInterstitialLoad()
        AdManager.showInterstitial(activity()) { closedCount++ }

        assertEquals(1, adMob.interstitialsShown)
        assertEquals(0, meta.interstitialsShown)
        assertEquals(0, closedCount)

        adMob.closeInterstitial()
        assertEquals(1, closedCount)
    }

    @Test
    fun show_fallsBackToMeta() {
        init()
        loadInterstitial()
        adMob.failInterstitialLoad()
        meta.finishInterstitialLoad()
        AdManager.showInterstitial(activity()) { closedCount++ }

        assertEquals(0, adMob.interstitialsShown)
        assertEquals(1, meta.interstitialsShown)

        meta.closeInterstitial()
        assertEquals(1, closedCount)
    }

    @Test
    fun show_withNothingReady_callsOnClosedImmediately() {
        init()
        AdManager.showInterstitial(activity()) { closedCount++ }

        assertEquals(1, closedCount)
        assertEquals(0, adMob.interstitialsShown)
        assertEquals(0, meta.interstitialsShown)
    }

    // ---------- Auto-reload ----------

    @Test
    fun afterAnAdCloses_theNextOneIsLoadedAutomatically() {
        init()
        loadInterstitial()
        adMob.finishInterstitialLoad()
        AdManager.showInterstitial(activity()) { closedCount++ }
        adMob.closeInterstitial()

        assertEquals(2, adMob.interstitialRequests)
    }

    @Test
    fun autoReloadOff_nothingIsLoadedAfterClose() {
        init(SdkConfig(autoInitialize = false, autoReload = false))
        loadInterstitial()
        adMob.finishInterstitialLoad()
        AdManager.showInterstitial(activity()) { closedCount++ }
        adMob.closeInterstitial()

        assertEquals(1, adMob.interstitialRequests)
    }

    @Test
    fun appReloadsInItsOwnCallback_stillOnlyOneNewRequest() {
        init()
        loadInterstitial()
        adMob.finishInterstitialLoad()
        AdManager.showInterstitial(activity()) { loadInterstitial() }
        adMob.closeInterstitial()

        assertEquals(2, adMob.interstitialRequests)
    }

    // ---------- Rewarded ----------

    @Test
    fun rewarded_fallsBackToMeta_andReportsRewardThenClose() {
        init()
        var rewards = 0
        AdManager.loadRewarded(context, "admob-id", "meta-id")
        adMob.failRewardedLoad()
        meta.finishRewardedLoad()
        assertTrue(AdManager.isRewardedReady())

        AdManager.showRewarded(activity(), { rewards++ }, { closedCount++ })
        assertEquals(0, adMob.rewardedShown)
        assertEquals(1, meta.rewardedShown)

        meta.earnReward()
        meta.closeRewarded()
        assertEquals(1, rewards)
        assertEquals(1, closedCount)
    }

    @Test
    fun rewarded_withNothingReady_callsOnClosedWithoutReward() {
        init()
        var rewards = 0
        AdManager.showRewarded(activity(), { rewards++ }, { closedCount++ })

        assertEquals(0, rewards)
        assertEquals(1, closedCount)
    }

    // ---------- Initialization ----------

    @Test
    fun initialize_withoutAdMobAppIdInManifest_startsOnlyMeta() {
        init(SdkConfig(metaAppId = "meta-app-id", autoInitialize = true))

        assertEquals(0, adMob.initializeCalls)
        assertEquals(1, meta.initializeCalls)
    }

    @Test
    fun initialize_twice_startsTheNetworksOnce() {
        init(SdkConfig(metaAppId = "meta-app-id", autoInitialize = true))
        AdManager.initialize(context)

        assertEquals(1, meta.initializeCalls)
    }
}
