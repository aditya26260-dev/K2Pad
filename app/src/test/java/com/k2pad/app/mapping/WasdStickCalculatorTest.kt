package com.k2pad.app.mapping

import org.junit.Assert.assertEquals
import org.junit.Test

class WasdStickCalculatorTest {

    private val epsilon = 0.001f

    @Test
    fun `no keys held is centered`() {
        val v = WasdStickCalculator.calculate(up = false, down = false, left = false, right = false)
        assertEquals(0f, v.x, epsilon)
        assertEquals(0f, v.y, epsilon)
    }

    @Test
    fun `W alone is full up`() {
        val v = WasdStickCalculator.calculate(up = true, down = false, left = false, right = false)
        assertEquals(0f, v.x, epsilon)
        assertEquals(-1f, v.y, epsilon)
    }

    @Test
    fun `D alone is full right`() {
        val v = WasdStickCalculator.calculate(up = false, down = false, left = false, right = true)
        assertEquals(1f, v.x, epsilon)
        assertEquals(0f, v.y, epsilon)
    }

    @Test
    fun `W plus D normalizes to approximately 0point707, negative 0point707`() {
        // The exact case required by the project brief (sections 10 and 29).
        val v = WasdStickCalculator.calculate(up = true, down = false, left = false, right = true)
        assertEquals(0.707f, v.x, epsilon)
        assertEquals(-0.707f, v.y, epsilon)
        assertEquals(1f, v.magnitude, epsilon)
    }

    @Test
    fun `releasing D while W remains held returns to full up`() {
        // Same required case, second half: after W+D, release D -> (0,-1).
        val afterRelease = WasdStickCalculator.calculate(up = true, down = false, left = false, right = false)
        assertEquals(0f, afterRelease.x, epsilon)
        assertEquals(-1f, afterRelease.y, epsilon)
    }

    @Test
    fun `opposite keys up and down cancel to zero`() {
        val v = WasdStickCalculator.calculate(up = true, down = true, left = false, right = false)
        assertEquals(0f, v.x, epsilon)
        assertEquals(0f, v.y, epsilon)
    }

    @Test
    fun `opposite keys left and right cancel to zero`() {
        val v = WasdStickCalculator.calculate(up = false, down = false, left = true, right = true)
        assertEquals(0f, v.x, epsilon)
        assertEquals(0f, v.y, epsilon)
    }

    @Test
    fun `all four keys held cancels to zero on both axes`() {
        val v = WasdStickCalculator.calculate(up = true, down = true, left = true, right = true)
        assertEquals(0f, v.x, epsilon)
        assertEquals(0f, v.y, epsilon)
    }

    @Test
    fun `every diagonal combination normalizes to unit magnitude`() {
        val diagonals = listOf(
            WasdStickCalculator.calculate(up = true, down = false, left = false, right = true),  // W+D
            WasdStickCalculator.calculate(up = true, down = false, left = true, right = false),  // W+A
            WasdStickCalculator.calculate(up = false, down = true, left = false, right = true),  // S+D
            WasdStickCalculator.calculate(up = false, down = true, left = true, right = false),  // S+A
        )
        for (v in diagonals) {
            assertEquals(1f, v.magnitude, epsilon)
        }
    }
}
