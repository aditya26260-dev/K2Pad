package com.k2pad.app.capture

import com.k2pad.app.mapping.ButtonAction
import com.k2pad.app.mapping.GamepadState
import com.k2pad.app.mapping.MappingEngine
import com.k2pad.app.mapping.MouseBindings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EvdevEventTranslatorTest {

    private val epsilon = 0.001f

    @Test
    fun `W key down and up drive the left stick through MappingEngine`() {
        val engine = MappingEngine()
        val translator = EvdevEventTranslator(engine)

        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_KEY, EvdevCodes.KEY_W, EvdevCodes.KEY_STATE_DOWN))
        assertEquals(-1f, engine.currentState().leftStickY, epsilon)

        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_KEY, EvdevCodes.KEY_W, EvdevCodes.KEY_STATE_UP))
        assertEquals(0f, engine.currentState().leftStickY, epsilon)
    }

    @Test
    fun `autorepeat is treated the same as still held`() {
        val engine = MappingEngine()
        val translator = EvdevEventTranslator(engine)

        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_KEY, EvdevCodes.KEY_W, EvdevCodes.KEY_STATE_DOWN))
        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_KEY, EvdevCodes.KEY_W, EvdevCodes.KEY_STATE_REPEAT))
        assertEquals(-1f, engine.currentState().leftStickY, epsilon)
    }

    @Test
    fun `BTN_LEFT maps to the right trigger through MappingEngine`() {
        val engine = MappingEngine()
        val translator = EvdevEventTranslator(engine)

        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_KEY, EvdevCodes.BTN_LEFT, EvdevCodes.KEY_STATE_DOWN))
        assertEquals(1f, engine.currentState().rightTrigger, epsilon)

        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_KEY, EvdevCodes.BTN_LEFT, EvdevCodes.KEY_STATE_UP))
        assertEquals(0f, engine.currentState().rightTrigger, epsilon)
    }

    @Test
    fun `separate REL_X and REL_Y events are buffered and only applied together at SYN_REPORT`() {
        val engine = MappingEngine()
        val translator = EvdevEventTranslator(engine)

        // A real mouse move arrives as X, then Y, then SYN — nothing should
        // reach the mouse processor before SYN_REPORT shows up.
        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_REL, EvdevCodes.REL_X, 10))
        assertEquals(0f, engine.currentState().rightStickX, epsilon)

        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_REL, EvdevCodes.REL_Y, 5))
        assertEquals(0f, engine.currentState().rightStickX, epsilon)

        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_SYN, EvdevCodes.SYN_REPORT, 0))
        assertTrue("expected rightward motion once SYN_REPORT flushed the buffered delta", engine.currentState().rightStickX > 0f)
    }

    @Test
    fun `a SYN_REPORT with no pending motion does not add another nudge`() {
        val engine = MappingEngine()
        val translator = EvdevEventTranslator(engine)

        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_REL, EvdevCodes.REL_X, 20))
        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_SYN, EvdevCodes.SYN_REPORT, 0))
        val afterFirstFlush = engine.currentState().rightStickX

        // No new REL events before this second SYN_REPORT.
        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_SYN, EvdevCodes.SYN_REPORT, 0))
        val afterSecondFlush = engine.currentState().rightStickX

        assertEquals(afterFirstFlush, afterSecondFlush, epsilon)
    }

    @Test
    fun `wheel up translates to onWheel and reaches a bound action`() {
        val engine = MappingEngine(mouseBindings = MouseBindings(wheelUp = ButtonAction.Y))
        val translator = EvdevEventTranslator(engine)

        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_REL, EvdevCodes.REL_WHEEL, 1))
        assertTrue(engine.currentState().y)
    }

    @Test
    fun `unrecognized key codes are ignored rather than affecting state`() {
        val engine = MappingEngine()
        val translator = EvdevEventTranslator(engine)
        translator.onRawEvent(RawInputEvent(EvdevCodes.EV_KEY, 999, EvdevCodes.KEY_STATE_DOWN))
        assertEquals(GamepadState.NEUTRAL, engine.currentState())
    }
}
