package com.k2pad.app.mapping

/**
 * Combines the WASD stick, mouse-driven right stick, and discrete
 * key/mouse-button/wheel bindings into a single [GamepadState].
 *
 * Deliberately knows nothing about *how* key/mouse events arrive (evdev,
 * AccessibilityService fallback, whatever) — Phase 3 owns translating raw
 * input into calls on this class. That keeps every rule here testable on
 * a plain JVM, with no Android/Shizuku/native dependency at all.
 */
class MappingEngine(
    private var keyBindings: KeyBindings = KeyBindings(),
    private var mouseBindings: MouseBindings = MouseBindings(),
    mouseSettings: MouseSettings = MouseSettings(),
) {
    private val heldKeys = mutableSetOf<LogicalKey>()
    private val heldMouseButtons = mutableSetOf<MouseButton>()
    private val mouseStick = MouseStickProcessor(mouseSettings)

    // Wheel ticks are momentary, like a real scroll-wheel click: a pulse
    // is "on" for exactly the next currentState() read, then clears
    // itself, rather than needing an explicit release call.
    private var wheelUpPulse = false
    private var wheelDownPulse = false

    fun updateKeyBindings(newBindings: KeyBindings) { keyBindings = newBindings }
    fun updateMouseBindings(newBindings: MouseBindings) { mouseBindings = newBindings }
    fun updateMouseSettings(newSettings: MouseSettings) { mouseStick.updateSettings(newSettings) }

    fun onKeyDown(key: LogicalKey) { heldKeys.add(key) }
    fun onKeyUp(key: LogicalKey) { heldKeys.remove(key) }

    fun onMouseButtonDown(button: MouseButton) { heldMouseButtons.add(button) }
    fun onMouseButtonUp(button: MouseButton) { heldMouseButtons.remove(button) }

    fun onMouseDelta(deltaX: Float, deltaY: Float) { mouseStick.onMouseDelta(deltaX, deltaY) }

    fun onWheel(direction: WheelDirection) {
        when (direction) {
            WheelDirection.UP -> wheelUpPulse = true
            WheelDirection.DOWN -> wheelDownPulse = true
        }
    }

    /** Advances the mouse spring-back decay; call once per input-processing tick. */
    fun tick(deltaTimeSeconds: Float) { mouseStick.tick(deltaTimeSeconds) }

    /**
     * Clears all held keys/buttons and snaps both sticks back to center.
     * Used on focus loss, service interruption, and the failsafe
     * (project brief sections 10, 15, 20) so nothing stays stuck pressed
     * regardless of what the physical keyboard/mouse are doing.
     */
    fun reset() {
        heldKeys.clear()
        heldMouseButtons.clear()
        wheelUpPulse = false
        wheelDownPulse = false
        mouseStick.reset()
    }

    fun currentState(): GamepadState {
        val leftStick = WasdStickCalculator.calculate(
            up = keyBindings.stickUp in heldKeys,
            down = keyBindings.stickDown in heldKeys,
            left = keyBindings.stickLeft in heldKeys,
            right = keyBindings.stickRight in heldKeys,
        )
        val rightStick = mouseStick.currentOutput()

        val pressed = mutableSetOf<ButtonAction>()
        for (key in heldKeys) keyBindings.actions[key]?.let { pressed += it }
        for (button in heldMouseButtons) mouseBindings.buttons[button]?.let { pressed += it }
        if (wheelUpPulse) mouseBindings.wheelUp?.let { pressed += it }
        if (wheelDownPulse) mouseBindings.wheelDown?.let { pressed += it }
        wheelUpPulse = false
        wheelDownPulse = false

        return GamepadState(
            leftStickX = leftStick.x,
            leftStickY = leftStick.y,
            rightStickX = rightStick.x,
            rightStickY = rightStick.y,
            leftTrigger = if (ButtonAction.LEFT_TRIGGER in pressed) 1f else 0f,
            rightTrigger = if (ButtonAction.RIGHT_TRIGGER in pressed) 1f else 0f,
            a = ButtonAction.A in pressed,
            b = ButtonAction.B in pressed,
            x = ButtonAction.X in pressed,
            y = ButtonAction.Y in pressed,
            lb = ButtonAction.LB in pressed,
            rb = ButtonAction.RB in pressed,
            back = ButtonAction.BACK in pressed,
            start = ButtonAction.START in pressed,
            l3 = ButtonAction.L3 in pressed,
            r3 = ButtonAction.R3 in pressed,
            dpadUp = ButtonAction.DPAD_UP in pressed,
            dpadDown = ButtonAction.DPAD_DOWN in pressed,
            dpadLeft = ButtonAction.DPAD_LEFT in pressed,
            dpadRight = ButtonAction.DPAD_RIGHT in pressed,
        )
    }
}
