# Billing-Library(Monetization Kit)

[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=24)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9%2B-blue.svg)](https://kotlinlang.org)
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

### Modern Stack: Built with Kotlin, Coroutines, and Jetpack Compose.

---

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
	        implementation("com.github.MohitAnuragi:Billing-Library:1.0.0")
}
```



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
# 💡 Usage
### 1. Banner Ads
Jetpack Compose:
```
AdBanner(
    modifier = Modifier.fillMaxWidth(),
    adUnitId = "ca-app-pub-3940256099942544/6300978111", // Your Banner ID
    type = BannerType.ADMOB // or BannerType.META
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

// 2. Show the Ad (e.g., on button click)
AdManager.showInterstitial(activity) {
    // Callback runs when Ad is closed OR if Ad failed to load
    // Navigate to next screen here
    startActivity(Intent(this, NextActivity::class.java))
}
```
### 3. Rewarded Ads
```
// 1. Load
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
```
### In-App Review:
```
// Trigger review flow (Only shows if Play Store criteria are met)
MonetizationKit.requestAppReview(activity)
```
# 🛡️ ProGuard / R8 (Release Builds)
If you use code shrinking (MinifyEnabled true), the library includes its own consumer-rules.pro automatically. However, if you face issues, add these rules to your app's proguard-rules.pro:
```
-keep class com.billing.core.** { *; }
-keep class com.billing.ads.** { *; }
-keep class com.billing.update.** { *; }
-keep class com.billing.review.** { *; }
```

# 🤝 Contribution
Contributions are welcome! Please fork the repository and submit a pull request for any enhancements or bug fixes.

# Contact
Email : anuragimohit468@gmail.com






