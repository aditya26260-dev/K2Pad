package com.k2pad.app.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseCurveTest {

    private val epsilon = 0.001f

    @Test
    fun `exponent of 1 is a no-op`() {
        val v = StickVector(0.6f, -0.2f)
        val shaped = ResponseCurve.apply(v, exponent = 1f)
        assertEquals(v.x, shaped.x, epsilon)
        assertEquals(v.y, shaped.y, epsilon)
    }

    @Test
    fun `zero magnitude stays zero regardless of exponent`() {
        val shaped = ResponseCurve.apply(StickVector.ZERO, exponent = 2.5f)
        assertEquals(0f, shaped.magnitude, epsilon)
    }

    @Test
    fun `full deflection stays full deflection regardless of exponent`() {
        val v = StickVector(1f, 0f)
        val shaped = ResponseCurve.apply(v, exponent = 2.5f)
        assertEquals(1f, shaped.magnitude, epsilon)
    }

    @Test
    fun `exponent greater than 1 shrinks mid-range magnitude but preserves direction`() {
        val v = StickVector(0.5f, 0f)
        val shaped = ResponseCurve.apply(v, exponent = 2f)
        // 0.5^2 = 0.25 — softer near the center than linear.
        assertEquals(0.25f, shaped.magnitude, epsilon)
        assertTrue("direction should be preserved", shaped.x > 0f)
        assertEquals(0f, shaped.y, epsilon)
    }

    @Test
    fun `exponent less than 1 boosts mid-range magnitude`() {
        val v = StickVector(0.25f, 0f)
        val shaped = ResponseCurve.apply(v, exponent = 0.5f)
        // 0.25^0.5 = 0.5 — twitchier near the center than linear.
        assertEquals(0.5f, shaped.magnitude, epsilon)
    }
}
