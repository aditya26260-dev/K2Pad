package com.k2pad.app.mapping

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A normalized 2D stick position. Both axes are meant to stay within
 * [-1f, 1f], and the vector's magnitude (distance from center) should
 * never exceed 1f — see [clampedToUnitCircle].
 *
 * Y follows the convention used throughout the project brief: negative Y
 * means "up"/"forward" (W -> (0,-1)), matching how stick axes are
 * reported by uinput/Android once we get to the backend phases.
 */
data class StickVector(val x: Float, val y: Float) {

    val magnitude: Float get() = hypot(x, y)

    /** Standard atan2(y, x) angle, in radians. */
    val angle: Float get() = atan2(y, x)

    /**
     * Rescales this vector so its magnitude never exceeds 1, preserving
     * direction. A raw sum like W+D has magnitude sqrt(2) ~= 1.414; this
     * brings it back onto the unit circle without distorting the angle —
     * required by project brief section 10 ("must produce approximately
     * (0.707, -0.707) rather than exceeding the stick's legal range").
     * Vectors already inside the unit circle are returned unchanged.
     */
    fun clampedToUnitCircle(): StickVector {
        val m = magnitude
        return if (m > 1f) StickVector(x / m, y / m) else this
    }

    /**
     * Rebuilds a vector at this vector's current angle but with
     * [newMagnitude] instead. If this vector is exactly zero, the angle
     * is undefined, so this always returns [ZERO] regardless of
     * [newMagnitude] — there's no direction to project a magnitude onto.
     */
    fun withMagnitude(newMagnitude: Float): StickVector {
        if (magnitude == 0f) return ZERO
        val a = angle
        return StickVector(cos(a) * newMagnitude, sin(a) * newMagnitude)
    }

    operator fun plus(other: StickVector) = StickVector(x + other.x, y + other.y)
    operator fun times(scalar: Float) = StickVector(x * scalar, y * scalar)

    companion object {
        val ZERO = StickVector(0f, 0f)
    }
}
