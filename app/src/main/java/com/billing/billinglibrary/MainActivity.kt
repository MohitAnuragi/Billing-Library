package com.billing.billinglibrary

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.billing.billinglibrary.ui.theme.BillingLibraryTheme
import com.billing.ads.AdManager
import com.billing.ads.banner.compose.AdBanner
import com.billing.ads.banner.view.BannerType
import com.billing.core.MonetizationKit
import com.billing.core.SdkConfig

class MainActivity : ComponentActivity() {


    private val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    private val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        val config = SdkConfig(
            adMobAppId = "ca-app-pub-3940256099942544~3347511713", // Test App ID
            isDebug = true,
            autoInitialize = true
        )
        MonetizationKit.init(this, config)

        setContent {
            BillingLibraryTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        // Optional: Add a TopBar or simply leave it
                    }
                ) { innerPadding ->
                    MonetizationTestScreen(
                        modifier = Modifier.padding(innerPadding),
                        bannerId = TEST_BANNER_ID,
                        interstitialId = TEST_INTERSTITIAL_ID,
                        rewardedId = TEST_REWARDED_ID
                    )
                }
            }
        }
    }
}

@Composable
fun MonetizationTestScreen(
    modifier: Modifier = Modifier,
    bannerId: String,
    interstitialId: String,
    rewardedId: String
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // Make it scrollable
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = "Monetization Kit Test", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)

        // --- 1. Banner Ad ---
        AdBanner(
            modifier = Modifier.fillMaxWidth(),
            adUnitId = bannerId,
            type = BannerType.ADMOB
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text("Ads", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

        // --- 2. Ads Controls ---
        Button(onClick = {
            Toast.makeText(context, "Loading Interstitial...", Toast.LENGTH_SHORT).show()
            AdManager.loadInterstitial(context, interstitialId, "")
        }) { Text("Load Interstitial") }

        Button(onClick = {
            activity?.let {
                AdManager.showInterstitial(it) {
                    Toast.makeText(context, "Interstitial Closed", Toast.LENGTH_SHORT).show()
                }
            }
        }) { Text("Show Interstitial") }

        Button(onClick = {
            Toast.makeText(context, "Loading Rewarded...", Toast.LENGTH_SHORT).show()
            AdManager.loadRewarded(context, rewardedId, "")
        }) { Text("Load Rewarded") }

        Button(onClick = {
            activity?.let {
                AdManager.showRewarded(it,
                    onRewardEarned = { Toast.makeText(context, "💰 Reward!", Toast.LENGTH_SHORT).show() },
                    onAdClosed = { Toast.makeText(context, "Rewarded Closed", Toast.LENGTH_SHORT).show() }
                )
            }
        }) { Text("Show Rewarded") }

        Spacer(modifier = Modifier.height(10.dp))

        // --- SEPARATOR ---
        // (You can use Divider() or just a Spacer)
        Text("Play Store Features", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)

        // --- 3. Play Store Features Controls (NEW) ---

        // RATE APP BUTTON
        Button(
            onClick = {
                if (activity != null) {
                    Toast.makeText(context, "Requesting Review...", Toast.LENGTH_SHORT).show()
                    // CALL THE NEW FUNCTION
                    MonetizationKit.requestAppReview(activity)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Rate App (In-App Review)")
        }

        // UPDATE APP BUTTON
        Button(
            onClick = {
                if (activity != null) {
                    Toast.makeText(context, "Checking for Updates...", Toast.LENGTH_SHORT).show()
                    // CALL THE NEW FUNCTION
                    MonetizationKit.checkForAppUpdate(activity, isFlexible = true)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Check Update (Flexible)")
        }
    }
}