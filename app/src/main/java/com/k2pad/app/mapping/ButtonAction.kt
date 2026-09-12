package com.k2pad.app.mapping

/**
 * A gamepad-side action a key/mouse-button/wheel-tick can be bound to.
 * Deliberately excludes the sticks themselves — those are always driven
 * by [WasdStickCalculator] / [MouseStickProcessor], never by a discrete
 * button binding.
 */
enum class ButtonAction {
    A, B, X, Y,
    LB, RB,
    BACK, START,
    L3, R3,
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    LEFT_TRIGGER, RIGHT_TRIGGER,
}
