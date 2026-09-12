package com.k2pad.app.mapping

/**
 * Converts four directional-key states into a normalized left-stick
 * vector (project brief section 10 — "WASD as a real analog stick").
 *
 * Deliberately pure and stateless: it only ever looks at the current
 * instantaneous key state, never at history. That single property is
 * what correctly handles every case the project brief calls out without
 * any special-casing:
 *  - key down / key up: caller re-invokes with the new state each time.
 *  - simultaneous keys (diagonals): both axes contribute, then the
 *    result is clamped back onto the unit circle (W+D -> ~(0.707,-0.707),
 *    not (1,-1)).
 *  - opposite-direction keys: up+down (or left+right) held together
 *    subtract to exactly zero on that axis — there's no special
 *    "cancel" logic, it falls out of the raw subtraction.
 *  - rapid key transitions: since nothing is accumulated over time,
 *    there's no state that can "miss" a transition — every call reflects
 *    exactly the key state at that instant.
 */
object WasdStickCalculator {
    fun calculate(up: Boolean, down: Boolean, left: Boolean, right: Boolean): StickVector {
        val rawX = (if (right) 1f else 0f) - (if (left) 1f else 0f)
        val rawY = (if (down) 1f else 0f) - (if (up) 1f else 0f)
        return StickVector(rawX, rawY).clampedToUnitCircle()
    }
}
