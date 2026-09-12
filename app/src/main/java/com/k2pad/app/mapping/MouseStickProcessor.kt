package com.k2pad.app.mapping

import kotlin.math.exp
import kotlin.math.hypot

/**
 * Turns relative mouse motion into a right-stick-like position.
 *
 * ALGORITHM, AND WHY (project brief section 11 asks for both): a
 * physical analog stick reports a *held position* — push it right and it
 * stays at, say, 80% right until you let go, driving a continuous camera
 * turn for as long as you hold it. A mouse only ever reports *relative*
 * deltas; it has no held position of its own. Naively accumulating every
 * delta forever (`position += delta`, never decaying) would mean the
 * emulated stick drifts further from center the longer you play and
 * never returns to center on its own — exactly what section 11 says not
 * to do, and it would leave the camera "stuck" turning even after you
 * stop moving the mouse.
 *
 * Instead this uses a spring-back/decay model, the same family of
 * technique existing mouse-to-joystick tools use: each mouse delta nudges
 * an internal accumulated position further from center (scaled by
 * sensitivity/acceleration), and every tick — with or without fresh
 * input — that position decays exponentially back toward zero. The
 * practical result: a quick flick gives a brief camera nudge that fades
 * fast (good for fine aim correction), continuously moving the mouse in
 * one direction keeps "topping up" the position so the turn sustains for
 * as long as you keep moving (the natural feel a third-person camera
 * wants), and the stick always settles back to dead center shortly after
 * the mouse stops — matching what a released physical stick does.
 *
 * Stateful and intentionally not thread-safe: meant to be owned and
 * driven by a single input-processing thread (project brief section 21,
 * keeping the input path off the UI thread).
 */
class MouseStickProcessor(private var settings: MouseSettings) {

    private var position = StickVector.ZERO
    private var smoothedDx = 0f
    private var smoothedDy = 0f

    fun updateSettings(newSettings: MouseSettings) {
        settings = newSettings
    }

    /** Feed one raw relative-motion event (EV_REL REL_X/REL_Y, in mouse counts). */
    fun onMouseDelta(rawDeltaX: Float, rawDeltaY: Float): StickVector {
        // 1) Smoothing: exponential moving average over raw deltas, to tame
        //    sensor jitter without adding much perceptible lag. smoothing=0
        //    makes alpha=1, i.e. a full pass-through with no filtering.
        val alpha = (1f - settings.smoothing).coerceIn(0.01f, 1f)
        smoothedDx += (rawDeltaX - smoothedDx) * alpha
        smoothedDy += (rawDeltaY - smoothedDy) * alpha

        // 2) Acceleration: fast flicks get a proportionally bigger nudge
        //    than slow, deliberate movements covering the same raw distance.
        val speed = hypot(smoothedDx, smoothedDy)
        val accelMultiplier = 1f + settings.acceleration * speed

        // 3) Per-axis sensitivity + inversion.
        val invX = if (settings.invertX) -1f else 1f
        val invY = if (settings.invertY) -1f else 1f
        val nudgeX = smoothedDx * settings.sensitivityX * accelMultiplier * invX
        val nudgeY = smoothedDy * settings.sensitivityY * accelMultiplier * invY

        position = (position + StickVector(nudgeX, nudgeY)).clampedToUnitCircle()
        return shapedOutput()
    }

    /**
     * Advances the spring-back decay by [deltaTimeSeconds] of real time
     * with no new mouse motion. Call this every input-processing tick
     * (project brief section 21) even when no mouse event arrived that
     * tick, or the stick will never return to center once nudged.
     */
    fun tick(deltaTimeSeconds: Float): StickVector {
        val decay = exp(-settings.decayRatePerSecond * deltaTimeSeconds)
        position = position * decay
        return shapedOutput()
    }

    /** Immediately snaps back to dead center — used by focus-loss/failsafe handling. */
    fun reset() {
        position = StickVector.ZERO
        smoothedDx = 0f
        smoothedDy = 0f
    }

    /** Re-reads the current shaped output without mutating any state. */
    fun currentOutput(): StickVector = shapedOutput()

    private fun shapedOutput(): StickVector {
        val afterDeadzone = Deadzone.apply(position, settings.deadzone)
        val afterCurve = ResponseCurve.apply(afterDeadzone, settings.responseCurveExponent)
        val cappedMagnitude = afterCurve.magnitude.coerceAtMost(settings.maxOutput)
        return afterCurve.withMagnitude(cappedMagnitude)
    }
}
