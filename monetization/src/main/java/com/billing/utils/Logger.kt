package com.billing.utils


import android.util.Log
import com.billing.core.MonetizationKit

internal object Logger {
    private const val TAG = "MonetizationLib"

    fun d(message: String) {
        if (MonetizationKit.config.isDebug) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }
}