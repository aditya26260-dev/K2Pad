package com.k2pad.app.mapping

/**
 * Canonical, backend-agnostic controller state (project brief section
 * 26). Every value here is normalized:
 *  - stick axes: -1f..1f, 0 = centered
 *  - triggers: 0f..1f, 0 = released, 1 = fully pulled
 *  - buttons/d-pad: plain booleans
 *
 * Converting these into whatever numeric ranges a specific backend's
 * uinput/HID report expects happens only at the backend boundary
 * (Phase 4+'s VirtualGamepadBackend implementations) — nothing in this
 * class, MappingEngine, or anything else in this package should ever
 * produce or consume raw device units.
 */
data class GamepadState(
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,

    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,

    val a: Boolean = false,
    val b: Boolean = false,
    val x: Boolean = false,
    val y: Boolean = false,

    val lb: Boolean = false,
    val rb: Boolean = false,

    val back: Boolean = false,
    val start: Boolean = false,
    val l3: Boolean = false,
    val r3: Boolean = false,

    val dpadUp: Boolean = false,
    val dpadDown: Boolean = false,
    val dpadLeft: Boolean = false,
    val dpadRight: Boolean = false,
) {
    companion object {
        /** All-neutral state: centered sticks, released triggers, nothing pressed. */
        val NEUTRAL = GamepadState()
    }
}
