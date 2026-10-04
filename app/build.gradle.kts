import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appVersion = "1.2.2"
val (vMajor, vMinor, vPatch) = appVersion.split(".").map(String::toInt)
val realApiBaseUrl = "https://il2statsapi.combatbox.net"
// Test hook (debug only): -Pcbonline.apiBaseUrl=... points the debug build at another host
val debugApiBaseUrl = providers.gradleProperty("cbonline.apiBaseUrl").getOrElse(realApiBaseUrl)
val realMissionUrl = "https://campaign-data.combatbox.net/mission-info-tempest.json"
// Test hook (debug only): -Pcbonline.missionUrl=... points the debug build's mission card elsewhere
val debugMissionUrl = providers.gradleProperty("cbonline.missionUrl").getOrElse(realMissionUrl)

// Release signing: real key from git-ignored keystore.properties; otherwise unsigned,
// unless -Pcbonline.debugSignRelease=true (local testing only, never publish that APK)
val keystoreProps = rootProject.file("keystore.properties")
val debugSignRelease = providers.gradleProperty("cbonline.debugSignRelease").orNull == "true"

android {
    namespace = "io.github.riaanjutte.cbonline"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.riaanjutte.cbonline"
        minSdk = 26
        targetSdk = 35
        versionName = appVersion
        versionCode = vMajor * 10000 + vMinor * 100 + vPatch
        manifestPlaceholders["appLabel"] = "CB Online"
    }

    signingConfigs {
        if (keystoreProps.exists()) create("release") {
            val p = Properties().apply { keystoreProps.inputStream().use(::load) }
            storeFile = file(p.getProperty("storeFile"))
            storePassword = p.getProperty("storePassword")
            keyAlias = p.getProperty("keyAlias")
            keyPassword = p.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            // Own app ID and launcher name, so a dev build installs next to the released app
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appLabel"] = "CB Online (dev)"
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
            buildConfigField("String", "MISSION_URL", "\"$debugMissionUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$realApiBaseUrl\"")
            buildConfigField("String", "MISSION_URL", "\"$realMissionUrl\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = when {
                keystoreProps.exists() -> signingConfigs.getByName("release")
                debugSignRelease -> signingConfigs.getByName("debug")
                else -> null
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
