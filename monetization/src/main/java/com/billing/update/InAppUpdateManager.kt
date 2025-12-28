package com.billing.update


import android.app.Activity
import android.content.IntentSender
import com.billing.utils.Logger
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateManager(private val activity: Activity) {

    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(activity)
    private val UPDATE_REQUEST_CODE = 500

    fun checkForUpdate(isFlexible: Boolean = true) {
        val updateType = if (isFlexible) AppUpdateType.FLEXIBLE else AppUpdateType.IMMEDIATE

        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(updateType)
            ) {
                try {
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        activity,
                        AppUpdateOptions.defaultOptions(updateType),
                        UPDATE_REQUEST_CODE
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Logger.e("Update Flow Failed: ${e.message}")
                }
            } else {
                Logger.d("No Update Available")
            }
        }
    }
}