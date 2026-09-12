package com.k2pad.app.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadzoneTest {

    private val epsilon = 0.001f

    @Test
    fun `input at or below threshold is fully suppressed`() {
        val v = StickVector(0.05f, 0f)
        val result = Deadzone.apply(v, threshold = 0.1f)
        assertEquals(0f, result.magnitude, epsilon)
    }

    @Test
    fun `input exactly at threshold is suppressed`() {
        val v = StickVector(0.1f, 0f)
        val result = Deadzone.apply(v, threshold = 0.1f)
        assertEquals(0f, result.magnitude, epsilon)
    }

    @Test
    fun `input just above threshold produces a small but nonzero output`() {
        val v = StickVector(0.11f, 0f)
        val result = Deadzone.apply(v, threshold = 0.1f)
        assertTrue("expected a small nonzero result just past the deadzone", result.magnitude > 0f)
        assertTrue("expected the result to still be small", result.magnitude < 0.2f)
    }

    @Test
    fun `full deflection still reaches magnitude 1 after rescaling`() {
        val v = StickVector(1f, 0f)
        val result = Deadzone.apply(v, threshold = 0.2f)
        assertEquals(1f, result.magnitude, epsilon)
    }

    @Test
    fun `zero threshold is a no-op`() {
        val v = StickVector(0.02f, 0f)
        val result = Deadzone.apply(v, threshold = 0f)
        assertEquals(0.02f, result.magnitude, epsilon)
    }
}
