package com.k2pad.app.mapping

import kotlin.math.pow

/**
 * Applies an exponential response curve to a stick's magnitude while
 * preserving its direction. Deliberately NOT applied per-axis — doing
 * that would turn a circular deflection range into a diamond/cross shape
 * and distort diagonal movement unevenly compared to cardinal movement.
 *
 * exponent = 1.0 is linear (no change, returned as-is with no math).
 * exponent > 1.0 gives finer control near center — small movements
 * produce proportionally smaller output — while full deflection (1.0)
 * still maps to full deflection (1.0). exponent < 1.0 does the
 * opposite (twitchier near center, for players who prefer that).
 */
object ResponseCurve {
    fun apply(vector: StickVector, exponent: Float): StickVector {
        if (exponent == 1f) return vector
        val m = vector.magnitude.coerceIn(0f, 1f)
        val shaped = m.pow(exponent)
        return vector.withMagnitude(shaped)
    }
}
