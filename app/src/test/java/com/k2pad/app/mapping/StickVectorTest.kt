package com.k2pad.app.mapping

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.sqrt

class StickVectorTest {

    private val epsilon = 0.001f

    @Test
    fun `vector already inside unit circle is unchanged`() {
        val v = StickVector(0.5f, -0.3f)
        val clamped = v.clampedToUnitCircle()
        assertEquals(0.5f, clamped.x, epsilon)
        assertEquals(-0.3f, clamped.y, epsilon)
    }

    @Test
    fun `diagonal vector is rescaled onto the unit circle`() {
        // raw (1,-1) has magnitude sqrt(2) ~= 1.414, outside the unit circle
        val v = StickVector(1f, -1f)
        val clamped = v.clampedToUnitCircle()
        val expected = (1f / sqrt(2f))
        assertEquals(expected, clamped.x, epsilon)
        assertEquals(-expected, clamped.y, epsilon)
        assertEquals(1f, clamped.magnitude, epsilon)
    }

    @Test
    fun `withMagnitude preserves angle`() {
        val v = StickVector(1f, 0f) // angle 0
        val rescaled = v.withMagnitude(0.5f)
        assertEquals(0.5f, rescaled.x, epsilon)
        assertEquals(0f, rescaled.y, epsilon)
    }

    @Test
    fun `withMagnitude on a zero vector stays zero regardless of requested magnitude`() {
        val rescaled = StickVector.ZERO.withMagnitude(0.7f)
        assertEquals(0f, rescaled.x, epsilon)
        assertEquals(0f, rescaled.y, epsilon)
    }
}
