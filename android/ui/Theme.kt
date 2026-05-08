package com.evertasktools.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Ever Task Tools Enterprise Color Palette
val MintPrimary = Color(0xFF3EB489)
val MintPrimaryDark = Color(0xFF2E9A72)
val EggshellBackground = Color(0xFFF5F5F0)
val EggshellSurface = Color(0xFFFAFAF8)
val MattBlackBackground = Color(0xFF1A1A1A)
val MattBlackSurface = Color(0xFF262626)
val LightTextColor = Color(0xFFF5F5F0)
val DarkTextColor = Color(0xFF1A1A1A)
val GrayTextColor = Color(0xFF6B6B6B)
val ErrorRed = Color(0xFFE53E3E)

private val LightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = LightTextColor,
    primaryContainer = MintPrimary.copy(alpha = 0.12f),
    onPrimaryContainer = MintPrimaryDark,
    secondary = EggshellBackground,
    onSecondary = DarkTextColor,
    secondaryContainer = EggshellSurface,
    onSecondaryContainer = DarkTextColor,
    tertiary = MintPrimaryDark,
    onTertiary = LightTextColor,
    tertiaryContainer = MintPrimaryDark.copy(alpha = 0.12f),
    onTertiaryContainer = MintPrimaryDark,
    background = EggshellBackground,
    onBackground = DarkTextColor,
    surface = EggshellSurface,
    onSurface = DarkTextColor,
    surfaceVariant = EggshellBackground,
    onSurfaceVariant = GrayTextColor,
    error = ErrorRed,
    onError = LightTextColor,
    outline = GrayTextColor.copy(alpha = 0.5f),
    outlineVariant = GrayTextColor.copy(alpha = 0.25f),
    scrim = DarkTextColor.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = MattBlackBackground,
    primaryContainer = MintPrimaryDark,
    onPrimaryContainer = MintPrimary,
    secondary = MattBlackSurface,
    onSecondary = LightTextColor,
    secondaryContainer = MattBlackSurface,
    onSecondaryContainer = LightTextColor,
    tertiary = MintPrimaryDark,
    onTertiary = MattBlackBackground,
    tertiaryContainer = MintPrimaryDark.copy(alpha = 0.2f),
    onTertiaryContainer = MintPrimary,
    background = MattBlackBackground,
    onBackground = LightTextColor,
    surface = MattBlackSurface,
    onSurface = LightTextColor,
    surfaceVariant = Color(0xFF333333),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = ErrorRed,
    onError = LightTextColor,
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF333333),
    scrim = Color.Black.copy(alpha = 0.7f)
)

@Composable
fun EverTaskToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EverTaskTypography,
        content = content
    )
}
