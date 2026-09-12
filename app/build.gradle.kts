plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.k2pad.app"

    // compileSdk is intentionally ahead of targetSdk: Compose 1.12 (bundled in the
    // 2026.08.00 BOM below) itself compiles against API 37, so we compile against 37
    // to resolve its APIs cleanly. See README.md for why targetSdk stays at 36.
    compileSdk = 37

    // Pinned to AGP 9.3.0's documented default NDK side-by-side version so the NDK
    // Gradle downloads matches what this AGP release was built/tested against.
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.k2pad.app"
        minSdk = 26

        // Held one API level behind compileSdk on purpose: Android 17 (API 37) removes
        // the developer opt-out for large-screen orientation/resizability restrictions.
        // We'll revisit this once the core input/backend engine (Phases 2-6) is done.
        targetSdk = 36

        versionCode = 1
        versionName = "0.1.0-phase1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-Wall", "-Wextra")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Phase 1 has nothing performance-sensitive yet; kept for later phases
            // where the input path (section 21 of the project brief) matters.
            isJniDebuggable = true
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            // No explicit `version = "..."` here on purpose: I could not verify a
            // single current "latest bundled CMake" figure with confidence, so this
            // intentionally lets the Android Gradle Plugin / SDK manager resolve its
            // own managed default rather than have me guess a number. If Android
            // Studio's first sync prompts to install a specific CMake version, that's
            // expected — accept it, and tell me the exact version it picked so I can
            // pin it explicitly from Phase 2 onward.
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
