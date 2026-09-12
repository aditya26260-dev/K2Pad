package com.k2pad.app.mapping

/**
 * All the mouse -> right-stick tuning knobs from project brief section 11.
 * Defaults aim for a reasonable GTA V / third-person-camera feel; every
 * field is meant to end up user-editable via Phase 9's ProfileManager —
 * nothing about these values is hard-coded into MappingEngine's logic.
 */
data class MouseSettings(
    val sensitivityX: Float = 1.0f,
    val sensitivityY: Float = 1.0f,
    /** 0 = no extra boost for fast flicks; higher = faster movements get proportionally more sensitivity. */
    val acceleration: Float = 0.0f,
    /** 0 = no smoothing (raw deltas pass straight through); closer to 1 = heavier smoothing, more lag. */
    val smoothing: Float = 0.2f,
    /** Radial deadzone, 0..1, applied to the accumulated stick magnitude. */
    val deadzone: Float = 0.05f,
    /** Exponent applied to magnitude after the deadzone; 1.0 = linear. */
    val responseCurveExponent: Float = 1.6f,
    /** Hard ceiling on output stick magnitude, 0..1. */
    val maxOutput: Float = 1.0f,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    /**
     * Spring-back rate, in the sense of exp(-decayRatePerSecond * dt):
     * how fast the emulated stick returns to center once the mouse stops
     * moving. Higher = snappier return to center, lower = more glide.
     * See MouseStickProcessor's class doc for why this exists instead of
     * plain accumulation.
     */
    val decayRatePerSecond: Float = 10f,
)
