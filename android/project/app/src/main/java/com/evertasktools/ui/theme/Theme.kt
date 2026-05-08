package com.evertasktools.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = MintGreen,
    onPrimary = MattBlack,
    primaryContainer = MintGreen.copy(alpha = 0.12f),
    onPrimaryContainer = MattBlack,
    secondary = LightSurface,
    onSecondary = MattBlack,
    secondaryContainer = LightSurface,
    onSecondaryContainer = MattBlack,
    tertiary = MintGreen,
    onTertiary = MattBlack,
    tertiaryContainer = MintGreen.copy(alpha = 0.08f),
    onTertiaryContainer = MattBlack,
    background = EggshellWhite,
    onBackground = MattBlack,
    surface = LightSurface,
    onSurface = MattBlack,
    surfaceVariant = EggshellWhite,
    onSurfaceVariant = MattBlack.copy(alpha = 0.7f),
    surfaceTint = MintGreen,
    inverseSurface = MattBlack,
    inverseOnSurface = EggshellWhite,
    error = ErrorRed,
    onError = EggshellWhite,
    errorContainer = ErrorRed.copy(alpha = 0.12f),
    onErrorContainer = MattBlack,
    outline = MattBlack.copy(alpha = 0.2f),
    outlineVariant = MattBlack.copy(alpha = 0.1f),
    scrim = MattBlack.copy(alpha = 0.5f),
    inversePrimary = MintGreen.copy(alpha = 0.8f),
    surfaceDim = EggshellWhite.copy(alpha = 0.9f),
    surfaceBright = LightSurface,
    surfaceContainerLowest = EggshellWhite,
    surfaceContainerLow = LightSurface.copy(alpha = 0.8f),
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurface,
    surfaceContainerHighest = LightSurface.copy(alpha = 0.95f)
)

private val DarkColorScheme = darkColorScheme(
    primary = MintGreen,
    onPrimary = MattBlack,
    primaryContainer = MintGreen.copy(alpha = 0.2f),
    onPrimaryContainer = EggshellWhite,
    secondary = DarkSurface,
    onSecondary = EggshellWhite,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = EggshellWhite,
    tertiary = MintGreen,
    onTertiary = MattBlack,
    tertiaryContainer = MintGreen.copy(alpha = 0.15f),
    onTertiaryContainer = EggshellWhite,
    background = MattBlack,
    onBackground = EggshellWhite,
    surface = DarkSurface,
    onSurface = EggshellWhite,
    surfaceVariant = MattBlack,
    onSurfaceVariant = EggshellWhite.copy(alpha = 0.7f),
    surfaceTint = MintGreen,
    inverseSurface = EggshellWhite,
    inverseOnSurface = MattBlack,
    error = ErrorRed,
    onError = EggshellWhite,
    errorContainer = ErrorRed.copy(alpha = 0.2f),
    onErrorContainer = EggshellWhite,
    outline = EggshellWhite.copy(alpha = 0.2f),
    outlineVariant = EggshellWhite.copy(alpha = 0.1f),
    scrim = MattBlack.copy(alpha = 0.7f),
    inversePrimary = MintGreen.copy(alpha = 0.8f),
    surfaceDim = MattBlack.copy(alpha = 0.9f),
    surfaceBright = DarkSurface,
    surfaceContainerLowest = MattBlack,
    surfaceContainerLow = DarkSurface.copy(alpha = 0.8f),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface,
    surfaceContainerHighest = DarkSurface.copy(alpha = 0.95f)
)

@Composable
fun EverTaskToolsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

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
        shapes = HeroShapes,
        content = content
    )
}
