package com.k2pad.app.backend

import com.k2pad.app.mapping.GamepadState

/**
 * Abstraction boundary between mapping/UI logic and however a virtual
 * gamepad actually gets created (project brief section 25 — UI and
 * mapping logic stay independent of the privileged/native implementation).
 * [ShizukuUinputBackend] is the only implementation so far; an
 * UnavailableBackend (for when Shizuku isn't connected) is a natural
 * future addition that fits this same seam without touching callers.
 */
interface VirtualGamepadBackend {

    /**
     * Attempts to create the virtual device. Returns null on success, or a
     * human-readable reason on failure — never throws.
     */
    fun start(): String?

    /** Pushes one state update to the virtual device. Safe to call every tick. */
    fun writeState(state: GamepadState)

    /** Releases to neutral and destroys the device. Safe to call even if start() failed or was never called. */
    fun stop()
}
