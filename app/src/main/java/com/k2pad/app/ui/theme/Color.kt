package com.k2pad.app.ui.theme

import androidx.compose.ui.graphics.Color

// K2Pad skews dark-first: it's meant to feel like a gaming utility running
// alongside/behind a full-screen game, not a typical light business-app UI.
val K2PadAccent = Color(0xFF7AD7A0)      // status-good / "controller active" green
val K2PadAccentDark = Color(0xFF3F9E6B)
val K2PadWarning = Color(0xFFE0B84A)     // "partially supported" / needs attention
val K2PadError = Color(0xFFE0645A)       // "backend blocked" / failsafe triggered

val K2PadSurfaceDark = Color(0xFF14161A)
val K2PadSurfaceDarkVariant = Color(0xFF1E2126)
val K2PadOnSurfaceDark = Color(0xFFE7E9EC)

val K2PadSurfaceLight = Color(0xFFFAFAFA)
val K2PadSurfaceLightVariant = Color(0xFFE8EAED)
val K2PadOnSurfaceLight = Color(0xFF1A1C1E)
