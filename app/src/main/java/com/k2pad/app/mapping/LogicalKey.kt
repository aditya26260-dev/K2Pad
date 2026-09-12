package com.k2pad.app.mapping

/**
 * Keyboard keys K2Pad knows how to bind, independent of any specific
 * platform keycode. Phase 3's evdev capture layer is responsible for
 * translating raw Linux key codes (KEY_W, KEY_SPACE, ...) into these —
 * MappingEngine and everything else in this package never sees a raw
 * keycode directly, which is what keeps all of this testable on a plain
 * JVM with zero Android/evdev dependency.
 */
enum class LogicalKey {
    W, A, S, D,
    SPACE, E, F, R,
    LEFT_SHIFT, LEFT_CTRL,
    TAB, ENTER, ESCAPE,
    DIGIT_1, DIGIT_2, DIGIT_3, DIGIT_4,
    ARROW_UP, ARROW_DOWN, ARROW_LEFT, ARROW_RIGHT,
}
