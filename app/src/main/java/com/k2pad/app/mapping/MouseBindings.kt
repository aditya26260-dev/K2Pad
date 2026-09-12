package com.k2pad.app.mapping

/**
 * Which mouse buttons (and, optionally, wheel directions) map to which
 * discrete gamepad action. `wheelUp`/`wheelDown` default to unbound
 * (null) — project brief section 13 only requires that the wheel be
 * mappable, not that it come bound to anything out of the box.
 */
data class MouseBindings(
    val buttons: Map<MouseButton, ButtonAction> = defaultButtons(),
    val wheelUp: ButtonAction? = null,
    val wheelDown: ButtonAction? = null,
) {
    companion object {
        /** Defaults from project brief section 12. */
        fun defaultButtons(): Map<MouseButton, ButtonAction> = mapOf(
            MouseButton.LEFT to ButtonAction.RIGHT_TRIGGER,
            MouseButton.RIGHT to ButtonAction.LEFT_TRIGGER,
            MouseButton.MIDDLE to ButtonAction.R3,
            MouseButton.BACK to ButtonAction.LB,
            MouseButton.FORWARD to ButtonAction.RB,
        )
    }
}
