# Billing-Library(Monetization Kit)

[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1%2B-blue.svg)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

it is a robust, production-ready Android library designed to simplify the integration of **AdMob**, **Meta Audience Network**, and **Google Play Services** (In-App Updates & Reviews). It provides a clean, unified Kotlin API that handles the complexities of ad loading, waterfall logic, and lifecycle management, supporting both **Jetpack Compose** and **XML** layouts.

---

## 🚀 Features
 ### AdMob & Meta (Facebook) Integration:

  1. Banner Ads (Auto-handling for XML & Compose)

  2. Interstitial Ads (Load & Show with callback)

  3. Rewarded Ads (Load & Show with callback)

  4. Waterfall Logic: Automatically tries AdMob first, then falls back to Meta if AdMob fails.

### Google Play Features:

  1. In-App Updates: Flexible (Background) & Immediate (Blocking) updates.

  2. In-App Reviews: Trigger the native Play Store rating dialog.

### Modern Stack: Built with Kotlin. Works with XML layouts and Jetpack Compose.

---

## ✅ Requirements

| | Minimum |
|---|---|
| `minSdk` | 24 |
| `compileSdk` | 34 |
| Kotlin | 2.1 |

## 📦 Installation

### Step 1. Add the JitPack Repository
Add this to your project-level `settings.gradle.kts` (or root `build.gradle`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } 
    }
}

```
### Step 2. Add the Dependency
Add this to your app-level build.gradle.kts:
```
dependencies {
	        implementation("com.github.MohitAnuragi:Billing-Library:1.0.5")
}
```
That line gives you everything. The library is published as two parts, so you can also choose:

| Your app | Dependency |
|---|---|
| XML layouts only (no Compose is downloaded) | `com.github.MohitAnuragi.Billing-Library:monetization:1.0.5` |
| Jetpack Compose, with the `AdBanner` composable | `com.github.MohitAnuragi.Billing-Library:monetization-compose:1.0.5` |




# 🛠️ Setup & Configuration
### 1. Update AndroidManifest.xml
You must add your AdMob App ID to your app's manifest. The library does not hardcode this to prevent conflicts.
```
<manifest ...>
    <application ...>

        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"/>

    </application>
</manifest>
```
### 2. Initialize the Library
Call init in your Application class or main Activity before using any features.
```
// In Application.onCreate() or MainActivity.onCreate()
val config = SdkConfig(
    adMobAppId = "ca-app-pub-3940256099942544~3347511713", // Your AdMob App ID
    metaAppId = "YOUR_META_APP_ID", // your Meta Id
    isDebug = true, // Set to 'false' for Production
    autoInitialize = true
)

MonetizationKit.init(this, config)
```
With `autoInitialize = true`, `init` also starts the AdMob and Meta SDKs for you.

### 3. Ask for consent (apps with EEA / UK users)
Set `requireConsent = true` in `SdkConfig`, then call this in your first Activity. It shows Google's consent form when one is needed.
```
ConsentManager.gatherConsent(this) { canRequestAds ->
    if (canRequestAds) {
        // Safe to load ads now
    }
}
```
# 💡 Usage
### 1. Banner Ads
Jetpack Compose (needs the `monetization-compose` part, included in the all-in-one line):
```
AdBanner(
    modifier = Modifier.fillMaxWidth(),
    adUnitId = "ca-app-pub-3940256099942544/6300978111", // Your Banner ID
    type = BannerType.ADMOB, // or BannerType.META
    size = BannerSize.ADAPTIVE // optional: full-width banner (default is BannerSize.STANDARD, 320x50)
)
```

XML Layout:
```
<com.billing.ads.banner.view.AdBannerView
    android:id="@+id/bannerView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"/>
```
```
// In Activity/Fragment
binding.bannerView.loadAd("YOUR_BANNER_ID", BannerType.ADMOB)
```
### 2. Interstitial Ads
```
// 1. Load the Ad (Call this early, e.g., in onCreate)
AdManager.loadInterstitial(context, "ADMOB_ID", "META_ID")

// Optional: get told when the ad is ready or failed
AdManager.loadInterstitial(context, "ADMOB_ID", "META_ID",
    onLoaded = { /* ad is ready */ },
    onFailed = { error -> /* no ad available */ }
)

// 2. Show the Ad (e.g., on button click)
AdManager.showInterstitial(activity) {
    // Callback runs when Ad is closed OR if Ad failed to load
    // Navigate to next screen here
    startActivity(Intent(this, NextActivity::class.java))
}
```
The library asks AdMob first and falls back to Meta only if AdMob has no ad. After an ad closes, the next one is loaded automatically (turn this off with `autoReload = false` in `SdkConfig`).
### 3. Rewarded Ads
```
// 1. Load (onLoaded / onFailed are optional here too)
AdManager.loadRewarded(context, "ADMOB_ID", "META_ID")

// 2. Show
AdManager.showRewarded(activity,
    onRewardEarned = {
        // User finished the video. Give coins/gems here!
        addCoins(100)
    },
    onAdClosed = {
        // Ad closed. Resume app flow.
    }
)
```
## 4. Play Store Features
### In-App Update:
```
// Check for update (Flexible downloads in background)
MonetizationKit.checkForAppUpdate(activity, isFlexible = true)

// Optional: follow the progress
MonetizationKit.checkForAppUpdate(activity) { status ->
    // UpdateStatus.NO_UPDATE, STARTED, DOWNLOADED, FAILED or CANCELED
}
```
A downloaded flexible update is installed automatically the next time the user leaves the app. To show your own "Restart" prompt instead, pass `onDownloaded = { ... }` and call `MonetizationKit.completeAppUpdate()`.
### In-App Review:
```
// Trigger review flow (Only shows if Play Store criteria are met)
MonetizationKit.requestAppReview(activity)

// Optional: know when the flow is over
MonetizationKit.requestAppReview(activity) { isSuccessful -> /* continue */ }
```
# 🛡️ ProGuard / R8 (Release Builds)
You don't need to add any rules. The library uses no reflection, and the one rule its dependencies need is applied to your app automatically (consumer-rules.pro).

# 🤝 Contribution
Contributions are welcome! Please fork the repository and submit a pull request for any enhancements or bug fixes.

# Contact
Email : anuragimohit468@gmail.com






