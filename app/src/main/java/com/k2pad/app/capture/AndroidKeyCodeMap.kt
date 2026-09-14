package com.k2pad.app.capture

import android.view.KeyEvent
import com.k2pad.app.mapping.LogicalKey

/**
 * Maps Android's OWN KeyEvent.KEYCODE_* constants to [LogicalKey].
 *
 * This is a deliberately SEPARATE table from [EvdevCodes] /
 * [EvdevEventTranslator]: Android's KeyEvent codes and raw Linux evdev
 * KEY_* codes are two different numbering systems that both originate
 * from the same physical key press, but are not interchangeable values.
 *
 * This table exists only for MainActivity's local preview screen, which
 * uses Android's own unprivileged, Activity-focus-only input APIs so you
 * can test the mapping engine with a real keyboard/mouse today — before
 * Phase 5 wires up the real system-wide evdev capture path that works
 * even while another app (GTA V) is in the foreground. Named SDK
 * constants are used throughout instead of raw integers specifically so
 * this can't silently drift from whatever Android actually defines them
 * as on a given API level.
 */
object AndroidKeyCodeMap {
    fun toLogicalKey(keyCode: Int): LogicalKey? = when (keyCode) {
        KeyEvent.KEYCODE_W -> LogicalKey.W
        KeyEvent.KEYCODE_A -> LogicalKey.A
        KeyEvent.KEYCODE_S -> LogicalKey.S
        KeyEvent.KEYCODE_D -> LogicalKey.D
        KeyEvent.KEYCODE_SPACE -> LogicalKey.SPACE
        KeyEvent.KEYCODE_E -> LogicalKey.E
        KeyEvent.KEYCODE_F -> LogicalKey.F
        KeyEvent.KEYCODE_R -> LogicalKey.R
        KeyEvent.KEYCODE_SHIFT_LEFT -> LogicalKey.LEFT_SHIFT
        KeyEvent.KEYCODE_CTRL_LEFT -> LogicalKey.LEFT_CTRL
        KeyEvent.KEYCODE_TAB -> LogicalKey.TAB
        KeyEvent.KEYCODE_ENTER -> LogicalKey.ENTER
        KeyEvent.KEYCODE_ESCAPE -> LogicalKey.ESCAPE
        KeyEvent.KEYCODE_1 -> LogicalKey.DIGIT_1
        KeyEvent.KEYCODE_2 -> LogicalKey.DIGIT_2
        KeyEvent.KEYCODE_3 -> LogicalKey.DIGIT_3
        KeyEvent.KEYCODE_4 -> LogicalKey.DIGIT_4
        KeyEvent.KEYCODE_DPAD_UP -> LogicalKey.ARROW_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> LogicalKey.ARROW_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> LogicalKey.ARROW_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> LogicalKey.ARROW_RIGHT
        else -> null
    }
}
