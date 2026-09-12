package com.k2pad.app.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MouseStickProcessorTest {

    private val epsilon = 0.01f

    // smoothing=0 and deadzone=0 isolate the specific thing each test cares
    // about from the EMA smoothing filter's startup transient and the
    // deadzone's rescaling, both covered by their own dedicated tests.
    private fun isolatedSettings(
        sensitivityX: Float = 1f,
        sensitivityY: Float = 1f,
        acceleration: Float = 0f,
        responseCurveExponent: Float = 1f,
        decayRatePerSecond: Float = 10f,
    ) = MouseSettings(
        sensitivityX = sensitivityX,
        sensitivityY = sensitivityY,
        acceleration = acceleration,
        smoothing = 0f,
        deadzone = 0f,
        responseCurveExponent = responseCurveExponent,
        maxOutput = 1f,
        decayRatePerSecond = decayRatePerSecond,
    )

    @Test
    fun `a mouse delta produces a nonzero stick output in the same direction`() {
        val processor = MouseStickProcessor(isolatedSettings())
        val output = processor.onMouseDelta(10f, 0f)
        assertTrue("expected rightward output", output.x > 0f)
        assertEquals(0f, output.y, epsilon)
    }

    @Test
    fun `higher sensitivity produces a larger output for the same raw delta`() {
        // Kept deliberately small enough that neither case saturates against
        // the unit-circle clamp in MouseStickProcessor — if both sensitivities
        // pushed the position past magnitude 1, they'd both clamp down to the
        // same (1,0) and this comparison could never distinguish them.
        val low = MouseStickProcessor(isolatedSettings(sensitivityX = 0.1f))
        val high = MouseStickProcessor(isolatedSettings(sensitivityX = 0.3f))
        val lowOut = low.onMouseDelta(2f, 0f)
        val highOut = high.onMouseDelta(2f, 0f)
        assertTrue(
            "low=${lowOut.x} high=${highOut.x}",
            highOut.x > lowOut.x
        )
    }

    @Test
    fun `invertX flips the output direction`() {
        val processor = MouseStickProcessor(
            isolatedSettings().copy(invertX = true)
        )
        val output = processor.onMouseDelta(10f, 0f)
        assertTrue("expected inverted (leftward) output", output.x < 0f)
    }

    @Test
    fun `output never exceeds max output`() {
        val processor = MouseStickProcessor(isolatedSettings(sensitivityX = 50f))
        val output = processor.onMouseDelta(500f, 500f)
        assertTrue(output.magnitude <= 1f + epsilon)
    }

    @Test
    fun `stick decays toward center on tick with no new input`() {
        val processor = MouseStickProcessor(isolatedSettings(decayRatePerSecond = 10f))
        val nudged = processor.onMouseDelta(10f, 0f)
        assertTrue("expected a nonzero nudge before decay", nudged.magnitude > 0f)

        val afterDecay = processor.tick(1f) // a full second of decay
        assertTrue(
            "expected the stick to have decayed noticeably toward center",
            afterDecay.magnitude < nudged.magnitude
        )
    }

    @Test
    fun `stick settles fully back to center after enough decay time with no new input`() {
        val processor = MouseStickProcessor(isolatedSettings(decayRatePerSecond = 10f))
        processor.onMouseDelta(10f, 0f)

        var last = processor.currentOutput()
        repeat(50) { last = processor.tick(0.1f) } // 5 seconds of decay total
        assertEquals(
            "mouse input must NOT accumulate forever — it should return to center once the mouse stops moving",
            0f, last.magnitude, epsilon
        )
    }

    @Test
    fun `continuously moving in one direction sustains output rather than decaying away`() {
        val processor = MouseStickProcessor(isolatedSettings(decayRatePerSecond = 10f))
        var last = StickVector.ZERO
        repeat(20) {
            last = processor.onMouseDelta(10f, 0f)
            processor.tick(0.01f)
        }
        assertTrue(
            "continuous movement in one direction should keep the stick deflected, not let it decay to zero",
            last.magnitude > 0.05f
        )
    }

    @Test
    fun `reset snaps immediately back to center`() {
        val processor = MouseStickProcessor(isolatedSettings())
        processor.onMouseDelta(50f, 50f)
        processor.reset()
        assertEquals(0f, processor.currentOutput().magnitude, epsilon)
    }
}
