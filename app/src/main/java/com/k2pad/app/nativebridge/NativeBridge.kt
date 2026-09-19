package com.k2pad.app.nativebridge

import com.k2pad.app.mapping.GamepadState

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

    /**
     * Configures capabilities and creates the real virtual gamepad on an
     * already-open, privileged file descriptor (from
     * [com.k2pad.app.backend.ShizukuUinputBackend]). Returns 0 on success,
     * -errno on failure — never throws.
     */
    fun uinputCreate(fd: Int): Int {
        if (!loaded) return -1
        return try {
            nativeUinputCreate(fd)
        } catch (t: Throwable) {
            -1
        }
    }

    /**
     * The actual "convert to native ranges only at the backend boundary"
     * step from project brief section 26: [state]'s normalized values get
     * scaled to uinput's real axis ranges and packed into a button bitmask
     * HERE, in the one place that's allowed to know both shapes — nothing
     * upstream of this (MappingEngine, the backend interface) or the
     * native code downstream deals with un-normalized values otherwise.
     */
    fun uinputWriteState(fd: Int, state: GamepadState): Int {
        if (!loaded) return -1
        return try {
            nativeUinputWriteState(
                fd,
                scaleStickAxis(state.leftStickX),
                scaleStickAxis(state.leftStickY),
                scaleStickAxis(state.rightStickX),
                scaleStickAxis(state.rightStickY),
                scaleTriggerAxis(state.leftTrigger),
                scaleTriggerAxis(state.rightTrigger),
                packButtons(state),
                dpadHatX(state),
                dpadHatY(state),
            )
        } catch (t: Throwable) {
            -1
        }
    }

    /** Releases to neutral, destroys the device, and closes [fd]. Never throws. */
    fun uinputDestroy(fd: Int): Int {
        if (!loaded) return -1
        return try {
            nativeUinputDestroy(fd)
        } catch (t: Throwable) {
            -1
        }
    }

    // -32768..32767, matching the range create_device() declares for
    // ABS_X/Y/RX/RY in uinput_backend.cpp — must stay in sync with it.
    private fun scaleStickAxis(value: Float): Int =
        (value.coerceIn(-1f, 1f) * 32767f).toInt()

    // 0..255, matching ABS_Z/RZ's declared range.
    private fun scaleTriggerAxis(value: Float): Int =
        (value.coerceIn(0f, 1f) * 255f).toInt()

    // Bit order (A,B,X,Y,LB,RB,BACK,START,L3,R3) must match kButtonCodes'
    // order in uinput_backend.cpp exactly — change one, change the other.
    private fun packButtons(state: GamepadState): Int {
        var mask = 0
        if (state.a) mask = mask or (1 shl 0)
        if (state.b) mask = mask or (1 shl 1)
        if (state.x) mask = mask or (1 shl 2)
        if (state.y) mask = mask or (1 shl 3)
        if (state.lb) mask = mask or (1 shl 4)
        if (state.rb) mask = mask or (1 shl 5)
        if (state.back) mask = mask or (1 shl 6)
        if (state.start) mask = mask or (1 shl 7)
        if (state.l3) mask = mask or (1 shl 8)
        if (state.r3) mask = mask or (1 shl 9)
        return mask
    }

    // Small enough that duplicating uinput_backend.cpp's dpad_to_hat_x/y
    // logic here (rather than a JNI round trip just for this) is the
    // simpler tradeoff — both sides cancel opposite-held pairs to 0.
    private fun dpadHatX(state: GamepadState): Int = when {
        state.dpadLeft && state.dpadRight -> 0
        state.dpadLeft -> -1
        state.dpadRight -> 1
        else -> 0
    }

    private fun dpadHatY(state: GamepadState): Int = when {
        state.dpadUp && state.dpadDown -> 0
        state.dpadUp -> -1
        state.dpadDown -> 1
        else -> 0
    }

    private external fun nativeGetStatus(): String
    private external fun nativeRunUinputSelfTest(): String
    private external fun nativeUinputCreate(fd: Int): Int
    private external fun nativeUinputWriteState(
        fd: Int,
        leftStickX: Int, leftStickY: Int,
        rightStickX: Int, rightStickY: Int,
        leftTrigger: Int, rightTrigger: Int,
        buttonsBitmask: Int, hatX: Int, hatY: Int,
    ): Int
    private external fun nativeUinputDestroy(fd: Int): Int
}
