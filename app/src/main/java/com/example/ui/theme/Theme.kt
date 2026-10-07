package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ChorDarkColorScheme = darkColorScheme(
    primary = ChorGold,
    onPrimary = Color(0xFF0A0F1D),
    primaryContainer = Color(0xFF3A2E0B),
    onPrimaryContainer = ChorGoldBright,
    secondary = ChorNeonEmerald,
    onSecondary = Color(0xFF041F10),
    secondaryContainer = Color(0xFF0B3B24),
    onSecondaryContainer = ChorNeonEmerald,
    tertiary = ChorNeonCyan,
    onTertiary = Color(0xFF00262B),
    tertiaryContainer = Color(0xFF0A3945),
    onTertiaryContainer = ChorNeonCyan,
    background = ChorMidnightBg,
    onBackground = ChorTextPrimary,
    surface = ChorSurfaceDark,
    onSurface = ChorTextPrimary,
    surfaceVariant = ChorSurfaceElevated,
    onSurfaceVariant = ChorTextSecondary,
    error = ChorDangerCrimson,
    onError = Color.White,
    outline = ChorCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChorDarkColorScheme,
        typography = Typography,
        content = content
    )
}
