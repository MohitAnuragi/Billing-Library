package com.billing.update


import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.IntentSender
import android.os.Bundle
import android.view.View
import com.billing.utils.Logger
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import java.lang.ref.WeakReference

enum class UpdateStatus {
    NO_UPDATE,   // Nothing to update (or the app was not installed from Google Play)
    STARTED,     // Google Play's update screen was opened
    DOWNLOADED,  // A flexible update finished downloading and is ready to install
    FAILED,
    CANCELED     // The user cancelled the download
}

class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = UpdateState.manager(activity)

    // onDownloaded: called when a flexible update is downloaded; show your own "Restart" prompt
    // and call completeUpdate(). When null, the update is installed the next time the app
    // goes to the background.
    @JvmOverloads
    fun checkForUpdate(
        isFlexible: Boolean = true,
        onDownloaded: (() -> Unit)? = null,
        onStatus: (UpdateStatus) -> Unit = {}
    ) {
        val updateType = if (isFlexible) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE
        UpdateState.register(activity, activity)

        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                when {
                    // Downloaded earlier (maybe in a previous session) but never installed
                    appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED -> {
                        UpdateState.setCallbacks(activity, onDownloaded, onStatus)
                        UpdateState.onDownloadFinished()
                    }

                    appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                        && appUpdateInfo.isUpdateTypeAllowed(updateType) ->
                        startFlow(appUpdateInfo, updateType, onDownloaded, onStatus)

                    // The user left an immediate update before it finished: continue it
                    !isFlexible && appUpdateInfo.updateAvailability() ==
                        UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS ->
                        startFlow(appUpdateInfo, updateType, onDownloaded, onStatus)

                    else -> {
                        Logger.d("No Update Available")
                        onStatus(UpdateStatus.NO_UPDATE)
                    }
                }
            }
            .addOnFailureListener { error ->
                Logger.e("Update Check Failed: ${error.message}")
                onStatus(UpdateStatus.FAILED)
            }
    }

    // Installs a downloaded flexible update. The app restarts.
    fun completeUpdate() {
        UpdateState.completeUpdate()
    }

    private fun startFlow(
        appUpdateInfo: AppUpdateInfo,
        updateType: Int,
        onDownloaded: (() -> Unit)?,
        onStatus: (UpdateStatus) -> Unit
    ) {
        try {
            val isStarted = appUpdateManager.startUpdateFlowForResult(
                appUpdateInfo,
                activity,
                AppUpdateOptions.defaultOptions(updateType),
                UpdateState.UPDATE_REQUEST_CODE
            )
            if (isStarted) {
                UpdateState.setCallbacks(activity, onDownloaded, onStatus)
                UpdateState.isImmediateInProgress = updateType == AppUpdateType.IMMEDIATE
                onStatus(UpdateStatus.STARTED)
            } else {
                Logger.e("Update Flow Failed To Start")
                onStatus(UpdateStatus.FAILED)
            }
        } catch (e: IntentSender.SendIntentException) {
            Logger.e("Update Flow Failed: ${e.message}")
            onStatus(UpdateStatus.FAILED)
        }
    }
}

// State shared by every InAppUpdateManager, because an update outlives the call that started it
internal object UpdateState : Application.ActivityLifecycleCallbacks {

    const val UPDATE_REQUEST_CODE = 500

    private var appUpdateManager: AppUpdateManager? = null
    private var isRegistered = false

    // Activities that are currently visible. 0 = the app is in the background.
    private var startedActivities = 0
    private var isChangingConfigurations = false

    private var isDownloaded = false
    var isImmediateInProgress = false
    private var wasPausedDuringImmediate = false

    // Callbacks of the Activity that started the update. Dropped when that Activity is destroyed.
    private var callbackOwner: WeakReference<Activity>? = null
    private var onDownloaded: (() -> Unit)? = null
    private var onStatus: ((UpdateStatus) -> Unit)? = null

    private val installListener = InstallStateUpdatedListener { state ->
        when (state.installStatus()) {
            InstallStatus.DOWNLOADED -> if (!isDownloaded) onDownloadFinished()
            InstallStatus.FAILED -> {
                Logger.e("Update Failed: error code ${state.installErrorCode()}")
                onStatus?.invoke(UpdateStatus.FAILED)
            }
            InstallStatus.CANCELED -> {
                Logger.d("Update Canceled")
                onStatus?.invoke(UpdateStatus.CANCELED)
            }
            else -> {}
        }
    }

    fun manager(context: Context): AppUpdateManager =
        appUpdateManager ?: AppUpdateManagerFactory.create(context.applicationContext).also {
            it.registerListener(installListener)
            appUpdateManager = it
        }

    internal fun setManagerForTest(manager: AppUpdateManager) {
        manager.registerListener(installListener)
        appUpdateManager = manager
    }

    internal fun resetForTest() {
        // The old manager belongs to the previous test's Application, so it is just dropped
        appUpdateManager = null
        isRegistered = false
        startedActivities = 0
        isChangingConfigurations = false
        isDownloaded = false
        isImmediateInProgress = false
        wasPausedDuringImmediate = false
        callbackOwner = null
        onDownloaded = null
        onStatus = null
    }

    // visibleActivity: the Activity making the call, used to get the count right
    // when this is registered after the app is already on screen
    fun register(context: Context, visibleActivity: Activity? = null) {
        if (isRegistered) return
        val application = context.applicationContext as? Application ?: return
        isRegistered = true

        val isOnScreen = visibleActivity?.window?.peekDecorView()?.windowVisibility == View.VISIBLE
        startedActivities = if (isOnScreen) 1 else 0
        application.registerActivityLifecycleCallbacks(this)
    }

    fun setCallbacks(owner: Activity, onDownloaded: (() -> Unit)?, onStatus: (UpdateStatus) -> Unit) {
        this.callbackOwner = WeakReference(owner)
        this.onDownloaded = onDownloaded
        this.onStatus = onStatus
    }

    fun onDownloadFinished() {
        Logger.d("Update Downloaded")
        isDownloaded = true
        onStatus?.invoke(UpdateStatus.DOWNLOADED)

        val appHandlesInstall = onDownloaded
        if (appHandlesInstall != null) {
            appHandlesInstall()
        } else if (startedActivities == 0) {
            completeUpdate()
        }
        // Otherwise it is installed in onActivityStopped, when the app goes to the background
    }

    fun completeUpdate() {
        val manager = appUpdateManager
        if (manager == null) {
            Logger.e("completeUpdate: no update has been checked yet")
            return
        }
        isDownloaded = false
        manager.completeUpdate().addOnFailureListener { error ->
            Logger.e("Update Install Failed: ${error.message}")
        }
    }

    // If the user comes back while an immediate update is still running, show its screen again
    private fun resumeImmediateUpdate(activity: Activity) {
        val manager = appUpdateManager ?: return
        manager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                try {
                    manager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.defaultOptions(AppUpdateType.IMMEDIATE),
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Logger.e("Update Flow Failed: ${e.message}")
                }
            } else {
                isImmediateInProgress = false
            }
        }
    }

    override fun onActivityStarted(activity: Activity) {
        // A rotation stops and starts the Activity without the app leaving the screen
        if (!isChangingConfigurations) startedActivities++
        isChangingConfigurations = false
    }

    override fun onActivityStopped(activity: Activity) {
        isChangingConfigurations = activity.isChangingConfigurations
        if (isChangingConfigurations) return

        startedActivities = maxOf(0, startedActivities - 1)
        // App is in the background: Google Play installs the update without showing any UI
        if (startedActivities == 0 && isDownloaded && onDownloaded == null) {
            completeUpdate()
        }
    }

    override fun onActivityPaused(activity: Activity) {
        if (isImmediateInProgress) wasPausedDuringImmediate = true
    }

    override fun onActivityResumed(activity: Activity) {
        if (isImmediateInProgress && wasPausedDuringImmediate) {
            wasPausedDuringImmediate = false
            resumeImmediateUpdate(activity)
        }
    }

    override fun onActivityDestroyed(activity: Activity) {
        if (callbackOwner?.get() === activity) {
            callbackOwner = null
            onDownloaded = null
            onStatus = null
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
}
