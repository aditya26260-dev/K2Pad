package com.k2pad.app.mapping

/**
 * Which logical keys drive the left stick, and which logical keys map to
 * which discrete gamepad action. Every field has a default but is meant
 * to be fully remappable — Phase 9's ProfileManager will construct new
 * KeyBindings instances from user edits; nothing here is hard-coded into
 * MappingEngine's logic.
 */
data class KeyBindings(
    val stickUp: LogicalKey = LogicalKey.W,
    val stickDown: LogicalKey = LogicalKey.S,
    val stickLeft: LogicalKey = LogicalKey.A,
    val stickRight: LogicalKey = LogicalKey.D,

    val actions: Map<LogicalKey, ButtonAction> = defaultActions(),
) {
    companion object {
        /**
         * GTA V-oriented defaults from project brief sections 9 and 17.
         * Starting points only — not guaranteed to match every GTA V
         * build/port, and every entry here is meant to be edited.
         */
        fun defaultActions(): Map<LogicalKey, ButtonAction> = mapOf(
            LogicalKey.SPACE to ButtonAction.A,
            LogicalKey.E to ButtonAction.B,
            LogicalKey.F to ButtonAction.X,
            LogicalKey.R to ButtonAction.Y,
            LogicalKey.LEFT_SHIFT to ButtonAction.LB,
            LogicalKey.LEFT_CTRL to ButtonAction.L3,
            LogicalKey.TAB to ButtonAction.BACK,
            LogicalKey.ENTER to ButtonAction.START,
        )
    }
}
