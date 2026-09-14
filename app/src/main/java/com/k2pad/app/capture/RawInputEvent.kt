package com.k2pad.app.capture

/**
 * One parsed evdev event. The kernel timestamp is intentionally dropped —
 * MappingEngine and everything downstream works off explicit tick() calls
 * (see project brief section 21 on keeping the input path simple and
 * off the UI thread), not event timestamps.
 */
data class RawInputEvent(val type: Int, val code: Int, val value: Int)
