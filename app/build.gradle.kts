plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// The release workflow sets this from GitHub secrets; without it a local build keeps the debug key.
val keystoreFile = providers.environmentVariable("KEYSTORE_FILE").orNull

android {
    namespace = "com.rfaizm.harmoniamusic"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rfaizm.harmoniamusic"
        minSdk = 24
        targetSdk = 37
        // Written plainly because F-Droid reads them with a regex and never runs Gradle (F2). A release bumps both:
        // the code is major * 10000 + minor * 100 + patch (VersionTest), and the tag must be "v" + versionName.
        versionCode = 301
        versionName = "0.3.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // The lyrics service lives here rather than in the code, so it can point elsewhere per build.
        buildConfigField("String", "LYRICS_BASE_URL", "\"https://lrclib.net/api/\"")
    }

    signingConfigs {
        if (keystoreFile != null) {
            // Once a keystore is given, a missing password fails the build instead of quietly using the debug key.
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    // AGP's dependency list is encrypted with a key only Google holds; F-Droid rejects it and Play doesn't need it (F3).
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // Lists the app's languages for Android 13+'s per-app language setting (L1); see res/resources.properties.
    androidResources {
        generateLocaleConfig = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.viewmodel.compose)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}