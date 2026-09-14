package com.k2pad.app.capture

import com.k2pad.app.mapping.LogicalKey
import com.k2pad.app.mapping.MappingEngine
import com.k2pad.app.mapping.MouseButton
import com.k2pad.app.mapping.WheelDirection

/**
 * Feeds parsed evdev [RawInputEvent]s into a [MappingEngine].
 *
 * Relative motion (EV_REL REL_X/REL_Y) is buffered until an EV_SYN
 * SYN_REPORT arrives before calling [MappingEngine.onMouseDelta] once
 * with the combined delta. This matters: a single physical mouse move
 * is typically reported as a separate REL_X event, a separate REL_Y
 * event, then one SYN_REPORT marking "this is now a consistent state
 * update" — delivering X and Y as two separate onMouseDelta calls
 * instead of one combined call would feed MouseStickProcessor's
 * smoothing filter two half-samples instead of one real sample,
 * skewing its behavior.
 *
 * Key, mouse-button, and wheel events are forwarded immediately — a
 * single button's up/down transition has no "half-updated" state the
 * way splitting X/Y motion would, so there's no correctness reason to
 * hold them back until SYN, only added latency.
 */
class EvdevEventTranslator(private val mappingEngine: MappingEngine) {

    private var pendingDx = 0f
    private var pendingDy = 0f
    private var hasPendingMotion = false

    fun onRawEvent(event: RawInputEvent) {
        when (event.type) {
            EvdevCodes.EV_KEY -> handleKey(event)
            EvdevCodes.EV_REL -> handleRel(event)
            EvdevCodes.EV_SYN -> handleSyn(event)
        }
    }

    private fun handleKey(event: RawInputEvent) {
        // KEY_STATE_REPEAT is treated the same as "still down" — MappingEngine's
        // held-key/button sets are idempotent, so re-adding an already-held
        // key or button is harmless.
        val isDown = event.value == EvdevCodes.KEY_STATE_DOWN || event.value == EvdevCodes.KEY_STATE_REPEAT

        val button = mouseButtonFor(event.code)
        if (button != null) {
            if (isDown) mappingEngine.onMouseButtonDown(button) else mappingEngine.onMouseButtonUp(button)
            return
        }

        val key = logicalKeyFor(event.code)
        if (key != null) {
            if (isDown) mappingEngine.onKeyDown(key) else mappingEngine.onKeyUp(key)
        }
        // Any other code (unbound keys, keys this project doesn't care
        // about) is silently ignored rather than crashing — evdev nodes
        // report every key on the device, not just the ones we bind.
    }

    private fun handleRel(event: RawInputEvent) {
        when (event.code) {
            EvdevCodes.REL_X -> {
                pendingDx += event.value
                hasPendingMotion = true
            }
            EvdevCodes.REL_Y -> {
                pendingDy += event.value
                hasPendingMotion = true
            }
            EvdevCodes.REL_WHEEL -> {
                if (event.value > 0) mappingEngine.onWheel(WheelDirection.UP)
                else if (event.value < 0) mappingEngine.onWheel(WheelDirection.DOWN)
            }
        }
    }

    private fun handleSyn(event: RawInputEvent) {
        if (event.code != EvdevCodes.SYN_REPORT) return
        if (hasPendingMotion) {
            mappingEngine.onMouseDelta(pendingDx, pendingDy)
            pendingDx = 0f
            pendingDy = 0f
            hasPendingMotion = false
        }
    }

    private fun logicalKeyFor(code: Int): LogicalKey? = when (code) {
        EvdevCodes.KEY_W -> LogicalKey.W
        EvdevCodes.KEY_A -> LogicalKey.A
        EvdevCodes.KEY_S -> LogicalKey.S
        EvdevCodes.KEY_D -> LogicalKey.D
        EvdevCodes.KEY_SPACE -> LogicalKey.SPACE
        EvdevCodes.KEY_E -> LogicalKey.E
        EvdevCodes.KEY_F -> LogicalKey.F
        EvdevCodes.KEY_R -> LogicalKey.R
        EvdevCodes.KEY_LEFTSHIFT -> LogicalKey.LEFT_SHIFT
        EvdevCodes.KEY_LEFTCTRL -> LogicalKey.LEFT_CTRL
        EvdevCodes.KEY_TAB -> LogicalKey.TAB
        EvdevCodes.KEY_ENTER -> LogicalKey.ENTER
        EvdevCodes.KEY_ESC -> LogicalKey.ESCAPE
        EvdevCodes.KEY_1 -> LogicalKey.DIGIT_1
        EvdevCodes.KEY_2 -> LogicalKey.DIGIT_2
        EvdevCodes.KEY_3 -> LogicalKey.DIGIT_3
        EvdevCodes.KEY_4 -> LogicalKey.DIGIT_4
        EvdevCodes.KEY_UP -> LogicalKey.ARROW_UP
        EvdevCodes.KEY_DOWN -> LogicalKey.ARROW_DOWN
        EvdevCodes.KEY_LEFT -> LogicalKey.ARROW_LEFT
        EvdevCodes.KEY_RIGHT -> LogicalKey.ARROW_RIGHT
        else -> null
    }

    private fun mouseButtonFor(code: Int): MouseButton? = when (code) {
        EvdevCodes.BTN_LEFT -> MouseButton.LEFT
        EvdevCodes.BTN_RIGHT -> MouseButton.RIGHT
        EvdevCodes.BTN_MIDDLE -> MouseButton.MIDDLE
        EvdevCodes.BTN_SIDE -> MouseButton.BACK
        EvdevCodes.BTN_EXTRA -> MouseButton.FORWARD
        else -> null
    }
}
