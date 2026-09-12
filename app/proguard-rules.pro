# Add project specific ProGuard rules here.
# Release minification is currently disabled (isMinifyEnabled = false in app/build.gradle.kts),
# so this file has no effect yet. It's kept in place so the release build type's
# proguardFiles(...) reference resolves, and so rules can be added here once we
# turn minification on (planned around Phase 10 — UI polish / performance pass).

# Keep JNI-facing classes/methods so R8 never renames anything native code calls by name.
-keepclasseswithmembers class com.k2pad.app.nativebridge.NativeBridge {
    native <methods>;
}
