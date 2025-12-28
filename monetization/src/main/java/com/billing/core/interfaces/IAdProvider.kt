package com.billing.core.interfaces


import android.app.Activity
import android.content.Context

interface IAdProvider {

    fun initialize(context: Context, appId: String)


    fun loadInterstitial(context: Context, adUnitId: String)


    fun showInterstitial(activity: Activity, onAdClosed: () -> Unit)

    fun loadRewarded(context: Context, adUnitId: String)


    fun showRewarded(activity: Activity, onRewardEarned: () -> Unit, onAdClosed: () -> Unit)

    fun isInterstitialReady(): Boolean
}