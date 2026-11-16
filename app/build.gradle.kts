import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Ads switch (WIZ_3APPS_MASTER_PLAN.md 4.1). Off by default: the SDK is linked but never touched.
// Debug test ads only with -PwizAdsEnabled=true. A release with ads on needs ads.properties.
val wizAdsEnabled = (project.findProperty("wizAdsEnabled") as String?) == "true"
val adsProps = Properties().apply {
    rootProject.file("ads.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "com.wizlabs.wizdipcharts"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wizlabs.wizdipcharts"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "1.8"
        // Ship only English resources (drops dozens of unused library translations).
        resourceConfigurations += listOf("en")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "WIZ_ADS_ENABLED", "$wizAdsEnabled")
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("KEYSTORE_FILE")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            // Google's public test IDs. Only reachable with -PwizAdsEnabled=true.
            buildConfigField("String", "ADMOB_APP_ID", "\"ca-app-pub-3940256099942544~3347511713\"")
            buildConfigField("String", "AD_BANNER", "\"ca-app-pub-3940256099942544/9214589741\"")
            buildConfigField("String", "AD_NATIVE", "\"ca-app-pub-3940256099942544/2247696110\"")
        }
        release {
            fun id(k: String) = if (!wizAdsEnabled) "" else
                adsProps.getProperty(k) ?: error("wizAdsEnabled=true but ads.properties has no '$k'")
            buildConfigField("String", "ADMOB_APP_ID", "\"${id("appId")}\"")
            buildConfigField("String", "AD_BANNER", "\"${id("banner")}\"")
            buildConfigField("String", "AD_NATIVE", "\"${id("native")}\"")
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)
    // Installs bundled baseline profiles on sideloaded installs too: faster start, smoother first scrolls.
    implementation("androidx.profileinstaller:profileinstaller:1.4.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    // Play In-App Updates and In-App Review (Java artifacts, used by com.wizlabs.update).
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:review:2.0.2")
    // GMA Next-Gen SDK and UMP, linked but switched off (wizAdsEnabled=false): no init, no requests.
    implementation("com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.6.0")
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")
    testImplementation("junit:junit:4.13.2")
}
