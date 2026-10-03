plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val appVersion = "1.0.0"
val (vMajor, vMinor, vPatch) = appVersion.split(".").map(String::toInt)
val realApiBaseUrl = "https://il2statsapi.combatbox.net"
// Test hook (debug only): -Pcbonline.apiBaseUrl=... points the debug build at another host
val debugApiBaseUrl = providers.gradleProperty("cbonline.apiBaseUrl").getOrElse(realApiBaseUrl)

android {
    namespace = "io.github.riaanjutte.cbonline"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.riaanjutte.cbonline"
        minSdk = 26
        targetSdk = 35
        versionName = appVersion
        versionCode = vMajor * 10000 + vMinor * 100 + vPatch
    }

    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"$debugApiBaseUrl\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"$realApiBaseUrl\"")
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
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}
