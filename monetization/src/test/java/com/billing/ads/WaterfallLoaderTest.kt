package com.billing.ads

import org.junit.Assert.assertEquals
import org.junit.Test

class WaterfallLoaderTest {

    private var isReady = false
    private var loadedCount = 0
    private val failures = mutableListOf<String>()

    private val loader = WaterfallLoader { isReady }

    // A fake network: counts its requests and keeps the callbacks so the test can answer later
    private class FakeNetwork {
        var requests = 0
        var onLoaded: () -> Unit = {}
        var onFailed: (String) -> Unit = {}

        val step: LoadStep = { loaded, failed ->
            requests++
            onLoaded = loaded
            onFailed = failed
        }
    }

    private fun load(vararg networks: FakeNetwork) =
        loader.load(networks.map { it.step }, { loadedCount++ }, { failures.add(it) })

    @Test
    fun firstNetworkLoads_secondIsNeverAsked() {
        val adMob = FakeNetwork()
        val meta = FakeNetwork()
        load(adMob, meta)
        adMob.onLoaded()

        assertEquals(1, adMob.requests)
        assertEquals(0, meta.requests)
        assertEquals(1, loadedCount)
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun firstNetworkFails_secondIsAskedAndLoads() {
        val adMob = FakeNetwork()
        val meta = FakeNetwork()
        load(adMob, meta)
        adMob.onFailed("AdMob no fill")
        meta.onLoaded()

        assertEquals(1, meta.requests)
        assertEquals(1, loadedCount)
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun allNetworksFail_callsOnFailedOnceWithLastError() {
        val adMob = FakeNetwork()
        val meta = FakeNetwork()
        load(adMob, meta)
        adMob.onFailed("AdMob no fill")
        meta.onFailed("Meta no fill")

        assertEquals(0, loadedCount)
        assertEquals(listOf("Meta no fill"), failures)
    }

    @Test
    fun alreadyReady_callsOnLoadedWithoutRequest() {
        isReady = true
        val adMob = FakeNetwork()
        load(adMob)

        assertEquals(0, adMob.requests)
        assertEquals(1, loadedCount)
    }

    @Test
    fun secondCallWhileLoading_sendsNoRequestAndBothCallersAreNotified() {
        val adMob = FakeNetwork()
        load(adMob)
        load(adMob)
        adMob.onLoaded()

        assertEquals(1, adMob.requests)
        assertEquals(2, loadedCount)
    }

    @Test
    fun secondCallWhileLoading_bothCallersGetTheFailure() {
        val adMob = FakeNetwork()
        load(adMob)
        load(adMob)
        adMob.onFailed("No fill")

        assertEquals(listOf("No fill", "No fill"), failures)
    }

    @Test
    fun afterAFailure_theNextCallSendsANewRequest() {
        val adMob = FakeNetwork()
        load(adMob)
        adMob.onFailed("No fill")
        load(adMob)

        assertEquals(2, adMob.requests)
    }

    @Test
    fun networkAnsweringTwice_isOnlyCountedOnce() {
        val adMob = FakeNetwork()
        val meta = FakeNetwork()
        load(adMob, meta)
        adMob.onFailed("AdMob no fill")
        adMob.onFailed("AdMob no fill again")

        assertEquals(1, meta.requests)
        assertEquals(emptyList<String>(), failures)
    }

    @Test
    fun noNetworks_callsOnFailedImmediately() {
        load()

        assertEquals(0, loadedCount)
        assertEquals(listOf("No ad unit ID provided"), failures)
    }
}
