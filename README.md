# Monetization Kit

Monetization Kit (repository: Billing-Library) is an Android library that wraps **Google AdMob**, **Meta Audience Network** and **Google Play** in-app updates and reviews behind one small Kotlin API. It handles ad loading, network fallback, consent, lifecycle and clean-up, and works with both XML layouts and Jetpack Compose.

## Features

- **Banner, interstitial and rewarded ads** from AdMob, with Meta Audience Network as a fallback.
- **Reliable loading:** AdMob is asked first and Meta only when AdMob has no ad. Repeated load calls never send duplicate requests, and the next ad is loaded automatically after one is shown. Expired ads are replaced.
- **Banners for XML and Jetpack Compose**, in fixed or adaptive size. Banners are paused and destroyed with the screen.
- **GDPR consent** through Google's User Messaging Platform (opt-in).
- **Google Play In-App Updates** (flexible and immediate). Downloaded updates are installed for you.
- **Google Play In-App Reviews.**
- **No ProGuard rules needed** in your app.

## Requirements

| Requirement | Minimum |
|---|---|
| `minSdk` | 24 |
| `compileSdk` | 34 |
| Kotlin | 2.1 (required by the AdMob SDK) |
| AdMob account | Required for AdMob ads |
| Meta Audience Network account | Optional, only for Meta ads |

## Installation

### 1. Add the JitPack repository

In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

If your project uses Groovy, add `maven { url 'https://jitpack.io' }` to `settings.gradle` instead.

### 2. Add the dependency

In your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.MohitAnuragi:Billing-Library:v1.0.5")
}
```

The library is published as two artifacts. The line above includes both. You can also pick one:

| Your app | Dependency |
|---|---|
| XML layouts only (Jetpack Compose is not downloaded) | `com.github.MohitAnuragi.Billing-Library:monetization:v1.0.5` |
| Jetpack Compose, with the `AdBanner` composable | `com.github.MohitAnuragi.Billing-Library:monetization-compose:v1.0.5` |

The AdMob and Meta SDKs are exposed as `api` dependencies, so you can also use their classes directly.

## Setup

### 1. Add your AdMob App ID to the manifest

The Google Mobile Ads SDK stops the app at start-up if this tag is missing.

```xml
<manifest>
    <application>
        <meta-data
            android:name="com.google.android.gms.ads.APPLICATION_ID"
            android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY" />
    </application>
</manifest>
```

### 2. Initialize the library

Call `init` once, in your `Application` class:

```kotlin
import com.billing.core.MonetizationKit
import com.billing.core.SdkConfig

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MonetizationKit.init(
            this,
            SdkConfig(
                metaAppId = "YOUR_META_APP_ID",   // optional, starts Meta at launch
                isDebug = BuildConfig.DEBUG
            )
        )
    }
}
```

Register the class in the manifest with `android:name=".MyApplication"`. With the default settings, `init` also starts the AdMob and Meta SDKs.

### Configuration

All `SdkConfig` fields are optional.

| Field | Default | Description |
|---|---|---|
| `adMobAppId` | `""` | Optional. AdMob reads its App ID from the manifest; if this value is set and differs, an error is logged. |
| `metaAppId` | `""` | If set, the Meta SDK starts at launch. If empty, it starts the first time a Meta ad is loaded. |
| `isDebug` | `false` | Prints debug logs under the Logcat tag `MonetizationLib`. Errors are always logged. |
| `autoInitialize` | `true` | Starts the ad SDKs during `init`. Set to `false` to call `AdManager.initialize(context)` yourself. |
| `autoReload` | `true` | Loads the next interstitial or rewarded ad automatically after one closes. |
| `requireConsent` | `false` | No ad SDK is started and no ad is loaded until consent is known. See [Consent](#consent-gdpr). |
| `testDeviceIds` | empty | AdMob test device IDs (the hashed ID AdMob prints in Logcat). Also used to test the consent form. |
| `metaTestDeviceIds` | empty | Meta test device hashes. |
| `tagForChildDirectedTreatment` | `null` | `true` or `false` to mark your app as directed at children. `null` means not specified. |
| `tagForUnderAgeOfConsent` | `null` | `true` or `false` to mark users as under the age of consent. `null` means not specified. |

## Consent (GDPR)

Apps with users in the EEA, the UK or Switzerland must ask for consent before showing personalized ads. `ConsentManager` wraps Google's User Messaging Platform. It is opt-in: if you never call it, nothing changes.

```kotlin
import com.billing.consent.ConsentManager

// Application: wait for consent before starting ads
MonetizationKit.init(this, SdkConfig(requireConsent = true))

// First Activity, in onCreate()
ConsentManager.gatherConsent(this) { canRequestAds ->
    if (canRequestAds) {
        AdManager.loadInterstitial(this, adMobId, metaId)
    }
}
```

| Function | Description |
|---|---|
| `gatherConsent(activity) { canRequestAds -> }` | Updates the consent status and shows Google's consent form when needed. The callback always runs. Starts the ad SDKs once ads are allowed. |
| `canRequestAds(context)` | `true` when ads may be requested. |
| `isPrivacyOptionsRequired(context)` | `true` when your app must offer a privacy settings entry. |
| `showPrivacyOptionsForm(activity) { }` | Shows the form again so the user can change their choice. |

Notes:

- With `requireConsent = true`, ad loads report a failure and banners stay empty until consent is known. Users who consented in an earlier session can be served immediately.
- You must create a GDPR message in your AdMob account under *Privacy & messaging*.
- To test the form outside Europe, set `isDebug = true` and add your device to `testDeviceIds`. The device is then treated as being in the EEA.

## Banner ads

### Jetpack Compose

```kotlin
import com.billing.ads.banner.compose.AdBanner
import com.billing.ads.banner.view.BannerSize
import com.billing.ads.banner.view.BannerType

AdBanner(
    modifier = Modifier.fillMaxWidth(),
    adUnitId = "YOUR_BANNER_AD_UNIT_ID",
    type = BannerType.ADMOB,        // or BannerType.META
    size = BannerSize.ADAPTIVE      // or BannerSize.STANDARD (default)
)
```

When `adUnitId`, `type` or `size` changes, a new banner is loaded.

### XML layout

```xml
<com.billing.ads.banner.view.AdBannerView
    android:id="@+id/bannerView"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:minHeight="50dp" />
```

```kotlin
binding.bannerView.loadAd("YOUR_BANNER_AD_UNIT_ID", BannerType.ADMOB, BannerSize.ADAPTIVE)
```

### Sizes

| Size | AdMob | Meta |
|---|---|---|
| `STANDARD` (default) | Fixed 320 x 50 dp | 50 dp high, full width |
| `ADAPTIVE` | Full width, height chosen by Google for the device | 90 dp high on tablets, 50 dp on phones |

Banners are destroyed when their view leaves the screen, reloaded if the same view returns (for example in a `RecyclerView`), and paused while the window is hidden. Call `destroy()` only if you want to remove a banner yourself.

## Interstitial ads

Load the ad early, then show it at a natural break in your app.

```kotlin
import com.billing.ads.AdManager

// Load. Pass "" as an ID to skip that network.
AdManager.loadInterstitial(
    context,
    adMobId = "YOUR_ADMOB_INTERSTITIAL_ID",
    metaId = "YOUR_META_PLACEMENT_ID",
    onLoaded = { /* optional: an ad is ready */ },
    onFailed = { error -> /* optional: no network had an ad */ }
)

// Show
AdManager.showInterstitial(activity) {
    // Runs when the ad closes, when it fails to show, or right away if no ad is ready
    startActivity(Intent(this, NextActivity::class.java))
}
```

`AdManager.isInterstitialReady()` tells you whether an ad is loaded.

## Rewarded ads

```kotlin
AdManager.loadRewarded(context, "YOUR_ADMOB_REWARDED_ID", "YOUR_META_PLACEMENT_ID")

AdManager.showRewarded(
    activity,
    onRewardEarned = { /* the user watched the full video: grant the reward */ },
    onAdClosed = { /* resume the app */ }
)
```

`onAdClosed` always runs, including when no ad was ready, so your app never waits forever. Only grant the reward in `onRewardEarned`. `AdManager.isRewardedReady()` tells you whether an ad is loaded.

## How ad loading works

- **Waterfall:** AdMob is asked first. Meta is asked only when AdMob has no ad. Showing follows the same order.
- **No duplicate requests:** if an ad is already loaded or a load is running, another load call sends no request; its callbacks receive the result of the running load.
- **Callbacks:** `onLoaded` runs once, as soon as an ad is ready. `onFailed` runs once, only when every requested network has failed.
- **Automatic reload:** after an ad closes, or when you try to show one and none is ready, the next ad is loaded with the IDs you used last. Disable this with `autoReload = false`.
- **Expiry:** AdMob ads are valid for one hour. Older ads are treated as not ready and replaced.

## In-app updates

```kotlin
import com.billing.update.UpdateStatus

// Flexible (default): the user keeps using the app while the update downloads
MonetizationKit.checkForAppUpdate(activity)

// Immediate: a full-screen flow that blocks the app until the update is installed
MonetizationKit.checkForAppUpdate(activity, isFlexible = false)
```

A downloaded flexible update is installed the next time the user leaves the app, with no interruption. To show your own prompt instead, pass `onDownloaded` and install when the user agrees:

```kotlin
MonetizationKit.checkForAppUpdate(
    activity,
    onDownloaded = { showRestartPrompt { MonetizationKit.completeAppUpdate() } },
    onStatus = { status -> /* optional progress reporting */ }
)
```

| `UpdateStatus` | Meaning |
|---|---|
| `NO_UPDATE` | No newer version, or the app was not installed from Google Play. |
| `STARTED` | Google Play's update flow was opened. |
| `DOWNLOADED` | A flexible update has finished downloading. |
| `FAILED` | The check, download or installation failed. Details are logged. |
| `CANCELED` | The user cancelled the download. |

The library also finishes updates downloaded in an earlier session and resumes an immediate update when the user returns to the app. In-app updates only work for apps installed from Google Play; use Internal App Sharing or Internal Testing to try them.

## In-app reviews

```kotlin
MonetizationKit.requestAppReview(activity) { isSuccessful ->
    // true: the review flow finished without an error
}
```

Google limits how often the rating dialog appears and never reports whether it was shown or whether the user left a rating. Call it after a positive moment in your app rather than from a "Rate us" button.

## Release builds (R8 / ProGuard)

No rules are needed in your app. The library uses no reflection, and the rules its dependencies need are applied to your app automatically.

## Testing

Use Google's test ad unit IDs during development. Never click live ads on your own device.

| Format | Test ID |
|---|---|
| App ID | `ca-app-pub-3940256099942544~3347511713` |
| Banner | `ca-app-pub-3940256099942544/6300978111` |
| Interstitial | `ca-app-pub-3940256099942544/1033173712` |
| Rewarded | `ca-app-pub-3940256099942544/5224354917` |

Set `isDebug = true` and filter Logcat by `MonetizationLib` to follow what the library does.

## Building from source

| Module | Description |
|---|---|
| `monetization` | The core library. |
| `monetization-compose` | The `AdBanner` composable. |
| `app` | Sample app that uses both modules with Google's test IDs. |

```bash
./gradlew :monetization:testDebugUnitTest   # unit tests (run on your computer)
./gradlew :app:installDebug                 # install the sample app on a device
./gradlew publishToMavenLocal               # publish both modules to ~/.m2
```

## Known limitations

- The library keeps one interstitial and one rewarded ad at a time.
- Banners do not fall back to the other network and have no load callbacks.
- If the user declines Google Play's first update dialog, the app receives `STARTED` and no further status.
- In-app updates and reviews only work for apps installed from Google Play.

## Contributing

Contributions are welcome. Please open an issue to discuss larger changes, then submit a pull request.

## Contact

Mohit Anuragi: anuragimohit468@gmail.com
