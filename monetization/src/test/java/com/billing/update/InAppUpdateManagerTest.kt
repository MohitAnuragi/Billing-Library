package com.billing.update

import android.app.Activity
import android.app.Application
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.billing.core.MonetizationKit
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController

@RunWith(RobolectricTestRunner::class)
class InAppUpdateManagerTest {

    private val context: Application = ApplicationProvider.getApplicationContext()
    private lateinit var fakePlay: FakeAppUpdateManager
    private lateinit var screen: ActivityController<Activity>

    private val statuses = mutableListOf<UpdateStatus>()
    private var downloadedCalls = 0

    @Before
    fun setUp() {
        MonetizationKit.resetForTest()
        UpdateState.resetForTest()

        fakePlay = FakeAppUpdateManager(context)
        UpdateState.setManagerForTest(fakePlay)
        // Same moment MonetizationKit.init registers it: before any Activity exists
        UpdateState.register(context)

        screen = Robolectric.buildActivity(Activity::class.java).setup()
    }

    private fun check(isFlexible: Boolean = true, onDownloaded: (() -> Unit)? = null) {
        InAppUpdateManager(screen.get()).checkForUpdate(isFlexible, onDownloaded) { statuses.add(it) }
        runPendingCallbacks()
    }

    private fun runPendingCallbacks() = shadowOf(Looper.getMainLooper()).idle()

    private fun userAcceptsAndDownloadFinishes() {
        fakePlay.userAcceptsUpdate()
        fakePlay.downloadStarts()
        fakePlay.downloadCompletes()
        runPendingCallbacks()
    }

    @Test
    fun noUpdate_reportsNoUpdate() {
        check()

        assertEquals(listOf(UpdateStatus.NO_UPDATE), statuses)
    }

    @Test
    fun flexibleUpdateAvailable_opensPlayDialogAndReportsStarted() {
        fakePlay.setUpdateAvailable(2)
        check()

        assertTrue(fakePlay.isConfirmationDialogVisible)
        assertEquals(listOf(UpdateStatus.STARTED), statuses)
    }

    @Test
    fun immediateUpdateAvailable_opensFullScreenFlow() {
        fakePlay.setUpdateAvailable(2)
        check(isFlexible = false)

        assertTrue(fakePlay.isImmediateFlowVisible)
        assertEquals(listOf(UpdateStatus.STARTED), statuses)
    }

    @Test
    fun downloadFinishes_reportsDownloaded() {
        fakePlay.setUpdateAvailable(2)
        check()
        userAcceptsAndDownloadFinishes()

        assertEquals(listOf(UpdateStatus.STARTED, UpdateStatus.DOWNLOADED), statuses)
    }

    @Test
    fun appHandlesInstall_nothingIsInstalledUntilCompleteUpdate() {
        fakePlay.setUpdateAvailable(2)
        check(onDownloaded = { downloadedCalls++ })
        userAcceptsAndDownloadFinishes()

        assertEquals(1, downloadedCalls)
        assertFalse(fakePlay.isInstallSplashScreenVisible)

        // Going to the background must not install either: the app said it handles it
        screen.pause().stop()
        assertFalse(fakePlay.isInstallSplashScreenVisible)

        InAppUpdateManager(screen.get()).completeUpdate()
        assertTrue(fakePlay.isInstallSplashScreenVisible)
    }

    @Test
    fun noCallback_updateIsInstalledWhenTheAppGoesToTheBackground() {
        fakePlay.setUpdateAvailable(2)
        check()
        userAcceptsAndDownloadFinishes()

        // Still on screen: do not interrupt the user
        assertFalse(fakePlay.isInstallSplashScreenVisible)

        screen.pause().stop()
        assertTrue(fakePlay.isInstallSplashScreenVisible)
    }

    @Test
    fun rotation_doesNotInstallTheUpdate() {
        fakePlay.setUpdateAvailable(2)
        check()
        userAcceptsAndDownloadFinishes()

        screen.recreate()

        assertFalse(fakePlay.isInstallSplashScreenVisible)
    }

    @Test
    fun userCancelsDownload_reportsCanceled() {
        fakePlay.setUpdateAvailable(2)
        check()
        fakePlay.userAcceptsUpdate()
        fakePlay.downloadStarts()
        fakePlay.userCancelsDownload()
        runPendingCallbacks()

        assertEquals(listOf(UpdateStatus.STARTED, UpdateStatus.CANCELED), statuses)
    }

    @Test
    fun downloadFails_reportsFailed() {
        fakePlay.setUpdateAvailable(2)
        check()
        fakePlay.userAcceptsUpdate()
        fakePlay.downloadStarts()
        fakePlay.downloadFails()
        runPendingCallbacks()

        assertEquals(listOf(UpdateStatus.STARTED, UpdateStatus.FAILED), statuses)
    }

    @Test
    fun downloadedInAnEarlierSession_isReportedOnTheNextCheck() {
        fakePlay.setUpdateAvailable(2)
        check()
        userAcceptsAndDownloadFinishes()
        statuses.clear()

        // The app is opened again later and checks once more
        check(onDownloaded = { downloadedCalls++ })

        assertEquals(listOf(UpdateStatus.DOWNLOADED), statuses)
        assertEquals(1, downloadedCalls)
    }
}
