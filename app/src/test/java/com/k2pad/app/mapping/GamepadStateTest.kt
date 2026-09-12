package com.k2pad.app.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GamepadStateTest {

    @Test
    fun `NEUTRAL has centered sticks, released triggers, and nothing pressed`() {
        val s = GamepadState.NEUTRAL
        assertEquals(0f, s.leftStickX)
        assertEquals(0f, s.leftStickY)
        assertEquals(0f, s.rightStickX)
        assertEquals(0f, s.rightStickY)
        assertEquals(0f, s.leftTrigger)
        assertEquals(0f, s.rightTrigger)
        assertFalse(s.a); assertFalse(s.b); assertFalse(s.x); assertFalse(s.y)
        assertFalse(s.lb); assertFalse(s.rb)
        assertFalse(s.back); assertFalse(s.start)
        assertFalse(s.l3); assertFalse(s.r3)
        assertFalse(s.dpadUp); assertFalse(s.dpadDown)
        assertFalse(s.dpadLeft); assertFalse(s.dpadRight)
    }

    @Test
    fun `default constructor equals NEUTRAL`() {
        assertEquals(GamepadState.NEUTRAL, GamepadState())
    }
}
