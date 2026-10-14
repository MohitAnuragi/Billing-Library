import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("maven-publish")
}

android {
    namespace = "com.billing.ads.compose"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
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
                artifactId = "monetization-compose"
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
    api(project(":monetization"))

    // "api": AdBanner has a Modifier parameter, so apps need the Compose UI classes
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
}
