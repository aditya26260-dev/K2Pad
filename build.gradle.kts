// Top-level build file where you can add configuration options common to all sub-projects/modules.
//
// NOTE: no org.jetbrains.kotlin.android plugin here. AGP 9+ has Kotlin
// support built in; explicitly applying that plugin is now a hard error
// ("The 'org.jetbrains.kotlin.android' plugin is no longer required for
// Kotlin support since AGP 9.0"), not just redundant. See app/build.gradle.kts
// for where jvmTarget now gets configured instead (kotlin.compilerOptions,
// not the old android.kotlinOptions).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
