package com.billing.review


import android.app.Activity
import com.billing.utils.Logger
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory

class InAppReviewManager internal constructor(
    private val activity: Activity,
    private val manager: ReviewManager
) {

    constructor(activity: Activity) : this(activity, ReviewManagerFactory.create(activity))

    // onComplete(true): the flow finished without an error. onComplete(false): it could not start.
    @JvmOverloads
    fun requestReview(onComplete: (Boolean) -> Unit = {}) {
        val request = manager.requestReviewFlow()

        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val reviewInfo = task.result
                val flow = manager.launchReviewFlow(activity, reviewInfo)
                flow.addOnCompleteListener { flowTask ->
                    // The flow has finished. The API does not indicate whether the user
                    // reviewed or not, or even if the review dialog was shown.
                    Logger.d("Review Flow Completed")
                    onComplete(flowTask.isSuccessful)
                }
            } else {
                Logger.e("Review Request Failed", task.exception)
                onComplete(false)
            }
        }
    }
}
