package com.billing.review


import android.app.Activity
import com.billing.utils.Logger
import com.google.android.play.core.review.ReviewManagerFactory

class InAppReviewManager(private val activity: Activity) {

    fun requestReview() {
        val manager = ReviewManagerFactory.create(activity)
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { _ ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even if the review dialog was shown.
                    Logger.d("Review Flow Completed")
                }
            } else {
                Logger.e("Review Request Failed")
            }
        }
    }
}