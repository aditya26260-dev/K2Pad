package com.k2pad.app.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingEngineTest {

    private val epsilon = 0.001f

    @Test
    fun `a fresh engine is fully neutral`() {
        val engine = MappingEngine()
        assertEquals(GamepadState.NEUTRAL, engine.currentState())
    }

    @Test
    fun `W drives the left stick fully up`() {
        val engine = MappingEngine()
        engine.onKeyDown(LogicalKey.W)
        val s = engine.currentState()
        assertEquals(0f, s.leftStickX, epsilon)
        assertEquals(-1f, s.leftStickY, epsilon)
    }

    @Test
    fun `W plus D diagonal normalizes on the left stick`() {
        val engine = MappingEngine()
        engine.onKeyDown(LogicalKey.W)
        engine.onKeyDown(LogicalKey.D)
        val s = engine.currentState()
        assertEquals(0.707f, s.leftStickX, epsilon)
        assertEquals(-0.707f, s.leftStickY, epsilon)
    }

    @Test
    fun `releasing a key returns the left stick to center on that axis`() {
        val engine = MappingEngine()
        engine.onKeyDown(LogicalKey.W)
        engine.onKeyUp(LogicalKey.W)
        val s = engine.currentState()
        assertEquals(0f, s.leftStickX, epsilon)
        assertEquals(0f, s.leftStickY, epsilon)
    }

    @Test
    fun `opposite direction keys cancel through the engine too`() {
        val engine = MappingEngine()
        engine.onKeyDown(LogicalKey.W)
        engine.onKeyDown(LogicalKey.S)
        val s = engine.currentState()
        assertEquals(0f, s.leftStickY, epsilon)
    }

    @Test
    fun `SPACE maps to the A button by default`() {
        val engine = MappingEngine()
        engine.onKeyDown(LogicalKey.SPACE)
        assertTrue(engine.currentState().a)
        engine.onKeyUp(LogicalKey.SPACE)
        assertFalse(engine.currentState().a)
    }

    @Test
    fun `left mouse button maps to the right trigger by default`() {
        val engine = MappingEngine()
        engine.onMouseButtonDown(MouseButton.LEFT)
        assertEquals(1f, engine.currentState().rightTrigger, epsilon)
        engine.onMouseButtonUp(MouseButton.LEFT)
        assertEquals(0f, engine.currentState().rightTrigger, epsilon)
    }

    @Test
    fun `left and right mouse buttons can both be held at once without conflict`() {
        val engine = MappingEngine()
        engine.onMouseButtonDown(MouseButton.LEFT)
        engine.onMouseButtonDown(MouseButton.RIGHT)
        val s = engine.currentState()
        assertEquals(1f, s.rightTrigger, epsilon)
        assertEquals(1f, s.leftTrigger, epsilon)
    }

    @Test
    fun `wheel binding fires as a one-shot pulse, not a held state`() {
        val engine = MappingEngine(mouseBindings = MouseBindings(wheelUp = ButtonAction.Y))
        engine.onWheel(WheelDirection.UP)
        assertTrue("first read after the wheel tick should show it pressed", engine.currentState().y)
        assertFalse("second read with no new wheel event should show it released", engine.currentState().y)
    }

    @Test
    fun `unbound wheel direction presses nothing`() {
        val engine = MappingEngine() // default MouseBindings has wheelUp/wheelDown = null
        engine.onWheel(WheelDirection.UP)
        val s = engine.currentState()
        assertEquals(GamepadState.NEUTRAL, s)
    }

    @Test
    fun `reset clears held keys, mouse buttons, and the mouse stick`() {
        val engine = MappingEngine()
        engine.onKeyDown(LogicalKey.W)
        engine.onKeyDown(LogicalKey.SPACE)
        engine.onMouseButtonDown(MouseButton.LEFT)
        engine.onMouseDelta(50f, 50f)

        engine.reset()

        assertEquals(GamepadState.NEUTRAL, engine.currentState())
    }

    @Test
    fun `rebinding a key changes which action it triggers`() {
        val engine = MappingEngine()
        val remapped = KeyBindings(
            actions = mapOf(LogicalKey.F to ButtonAction.B) // was X by default
        )
        engine.updateKeyBindings(remapped)
        engine.onKeyDown(LogicalKey.F)
        val s = engine.currentState()
        assertTrue(s.b)
        assertFalse(s.x)
    }

    @Test
    fun `rebinding the stick keys changes which physical keys drive the left stick`() {
        val engine = MappingEngine()
        val arrowKeys = KeyBindings(
            stickUp = LogicalKey.ARROW_UP,
            stickDown = LogicalKey.ARROW_DOWN,
            stickLeft = LogicalKey.ARROW_LEFT,
            stickRight = LogicalKey.ARROW_RIGHT,
        )
        engine.updateKeyBindings(arrowKeys)

        // W no longer does anything for the stick under this binding.
        engine.onKeyDown(LogicalKey.W)
        assertEquals(0f, engine.currentState().leftStickY, epsilon)

        engine.onKeyUp(LogicalKey.W)
        engine.onKeyDown(LogicalKey.ARROW_UP)
        assertEquals(-1f, engine.currentState().leftStickY, epsilon)
    }
}
