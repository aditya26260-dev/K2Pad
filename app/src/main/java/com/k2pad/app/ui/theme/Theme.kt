package com.k2pad.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val K2PadDarkColorScheme = darkColorScheme(
    primary = K2PadAccent,
    secondary = K2PadAccentDark,
    error = K2PadError,
    background = K2PadSurfaceDark,
    surface = K2PadSurfaceDark,
    surfaceVariant = K2PadSurfaceDarkVariant,
    onBackground = K2PadOnSurfaceDark,
    onSurface = K2PadOnSurfaceDark,
)

private val K2PadLightColorScheme = lightColorScheme(
    primary = K2PadAccentDark,
    secondary = K2PadAccent,
    error = K2PadError,
    background = K2PadSurfaceLight,
    surface = K2PadSurfaceLight,
    surfaceVariant = K2PadSurfaceLightVariant,
    onBackground = K2PadOnSurfaceLight,
    onSurface = K2PadOnSurfaceLight,
)

/**
 * K2Pad's Material3 theme. Supports Android 12+ dynamic color as a bonus,
 * but always has a sensible fixed fallback since this app also needs to look
 * right on Android 8-11 devices per the "other modern Android devices" goal
 * in the project brief.
 */
@Composable
fun K2PadTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> K2PadDarkColorScheme
        else -> K2PadLightColorScheme
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        val currentWindow = (view.context as? Activity)?.window
        if (currentWindow != null) {
            androidx.core.view.WindowCompat.getInsetsController(currentWindow, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = K2PadTypography,
        content = content
    )
}
