import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("maven-publish")
}

android {
    namespace = "com.billing.ads"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            // Needed by Robolectric
            isIncludeAndroidResources = true
        }
    }
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
        // Kept low so the library never raises an app's Kotlin requirement by itself
        // (the AdMob SDK is what sets the real minimum: Kotlin 2.1)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
    }
    coreLibrariesVersion = "2.0.21"
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = providers.gradleProperty("GROUP").get()
                artifactId = "monetization"
                // JitPack sets VERSION to the Git tag it is building. Using it here means
                // the two modules always point at each other with the right version.
                version = providers.environmentVariable("VERSION")
                    .filter { providers.environmentVariable("JITPACK").orNull == "true" }
                    .orElse(providers.gradleProperty("VERSION_NAME"))
                    .get()
            }
        }
    }
}

dependencies {
    // "api": apps that use the library can also use the AdMob and Meta classes directly
    api(libs.play.services.ads)
    api(libs.audience.network)

    // Consent (GDPR) for ConsentManager
    implementation(libs.ump)

    // Play Core: In-App Updates & Reviews
    implementation(libs.play.app.update)
    implementation(libs.play.review)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
