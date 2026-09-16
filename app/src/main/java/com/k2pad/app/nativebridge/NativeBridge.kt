package com.k2pad.app.nativebridge

/**
 * Thin bridge to the native (C++) layer.
 *
 * Phase 1 purpose: prove the Kotlin <-> JNI <-> CMake <-> NDK toolchain is
 * wired correctly end to end, with a single harmless round trip.
 *
 * Phase 4 adds [runUinputSelfTest], which drives the real uinput protocol
 * (open/configure/create/write/destroy) exactly as project brief section
 * 29 asks for. It uses a direct, almost-certainly-unprivileged open of
 * /dev/uinput — this is a DIAGNOSTIC, not the production path. The
 * production path (Phase 5/6) gets a privileged file descriptor from
 * Shizuku instead of opening the device itself.
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

    /**
     * Runs the native uinput self-test (open, configure capabilities,
     * create device, write a test button + stick event, release, destroy)
     * and returns a line-by-line report of what happened at each step.
     * Never throws. On a normal (non-Shizuku, non-root) app process this
     * is expected to fail at step 1 with a permission error — that is
     * itself a valid, useful diagnostic result, not a bug.
     */
    fun runUinputSelfTest(): String {
        if (!loaded) {
            return "native library not loaded: ${loadError?.message ?: "unknown error"}"
        }
        return try {
            nativeRunUinputSelfTest()
        } catch (t: Throwable) {
            "native call failed: ${t.message ?: t::class.simpleName}"
        }
    }

    private external fun nativeGetStatus(): String
    private external fun nativeRunUinputSelfTest(): String
}
