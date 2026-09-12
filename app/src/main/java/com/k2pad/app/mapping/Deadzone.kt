package com.k2pad.app.mapping

/**
 * Radial deadzone with rescaling: inputs at or below [threshold]
 * magnitude become exactly zero, and everything above threshold is
 * rescaled so the full 0..1 output range is still reachable.
 *
 * Rescaling matters: naively just clamping-below-threshold-to-zero and
 * passing everything else through unchanged leaves a "dead gap"
 * immediately outside the deadzone before any noticeable stick movement
 * appears, and means max physical input can never reach magnitude 1.
 * Rescaling avoids both problems.
 */
object Deadzone {
    fun apply(vector: StickVector, threshold: Float): StickVector {
        if (threshold <= 0f) return vector
        val m = vector.magnitude
        if (m <= threshold) return StickVector.ZERO
        val rescaled = ((m - threshold) / (1f - threshold)).coerceIn(0f, 1f)
        return vector.withMagnitude(rescaled)
    }
}
