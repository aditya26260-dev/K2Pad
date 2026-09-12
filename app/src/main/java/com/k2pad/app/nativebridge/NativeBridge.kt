package com.k2pad.app.nativebridge

/**
 * Thin bridge to the native (C++) layer.
 *
 * Phase 1 purpose: prove the Kotlin <-> JNI <-> CMake <-> NDK toolchain is
 * wired correctly end to end, with a single harmless round trip. It does NOT
 * touch uinput or evdev yet — that's Phase 4 (native uinput backend) and
 * Phase 5-6 (Shizuku-hosted evdev/uinput UserService).
 *
 * UI code should only ever go through this object (or its future
 * VirtualGamepadBackend replacement), never call System.loadLibrary or
 * declare `external fun` anywhere else — see project brief section 7,
 * "the UI must not directly manipulate low-level uinput code".
 */
object NativeBridge {

    private var loaded = false
    private var loadError: Throwable? = null

    init {
        try {
            System.loadLibrary("k2pad_native")
            loaded = true
        } catch (t: UnsatisfiedLinkError) {
            loadError = t
        }
    }

    /** True if the native library was found and linked successfully. */
    fun isLoaded(): Boolean = loaded

    /** Non-null only when [isLoaded] is false; useful for the Diagnostics screen later. */
    fun loadErrorMessage(): String? = loadError?.message

    /**
     * Returns a short human-readable status string from native code, or a
     * clear error string if the library never loaded. Never throws.
     */
    fun getNativeStatus(): String {
        if (!loaded) {
            return "native library not loaded: ${loadError?.message ?: "unknown error"}"
        }
        return try {
            nativeGetStatus()
        } catch (t: Throwable) {
            "native call failed: ${t.message ?: t::class.simpleName}"
        }
    }

    private external fun nativeGetStatus(): String
}
