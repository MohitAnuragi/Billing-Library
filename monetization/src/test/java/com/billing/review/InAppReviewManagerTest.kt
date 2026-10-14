package com.billing.review

import android.app.Activity
import android.os.Looper
import com.billing.core.MonetizationKit
import com.google.android.play.core.review.testing.FakeReviewManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class InAppReviewManagerTest {

    @Before
    fun setUp() {
        MonetizationKit.resetForTest()
    }

    @Test
    fun reviewFlowFinishes_callsOnCompleteWithTrue() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val results = mutableListOf<Boolean>()

        InAppReviewManager(activity, FakeReviewManager(activity)).requestReview { results.add(it) }
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf(true), results)
    }
}
