plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The release workflow sets these: the upload key from GitHub secrets, and the tag being released.
// Without them a local build keeps the debug key and version 1.0.
val keystoreFile = providers.environmentVariable("KEYSTORE_FILE").orNull
val versionTag = providers.environmentVariable("VERSION_TAG").orNull

/** "v1.2.3" gives "1.2.3" and 10203. Minor and patch stay under 100 so every newer tag has a bigger code, as Play needs. */
fun versionFromTag(tag: String): Pair<String, Int> {
    val (major, minor, patch) = Regex("""v(\d+)\.(\d{1,2})\.(\d{1,2})""").matchEntire(tag)?.destructured
        ?: error("VERSION_TAG must look like v1.2.3, got \"$tag\"")
    return "$major.$minor.$patch" to major.toInt() * 10_000 + minor.toInt() * 100 + patch.toInt()
}

android {
    namespace = "com.rfaizm.harmoniamusic"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.rfaizm.harmoniamusic"
        minSdk = 24
        targetSdk = 37
        val (name, code) = versionTag?.let { versionFromTag(it) } ?: ("1.0" to 1)
        versionCode = code
        versionName = name

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}