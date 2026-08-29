package com.peeratvoip.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val LocalNeuPalette = staticCompositionLocalOf { LightNeuPalette }

private val DarkColors = darkColorScheme(
    primary = DarkNeuPalette.accent,
    secondary = DarkNeuPalette.accentSecondary,
    background = DarkNeuPalette.background,
    surface = DarkNeuPalette.surface,
    onBackground = DarkNeuPalette.textPrimary,
    onSurface = DarkNeuPalette.textPrimary,
    error = DarkNeuPalette.danger,
)

private val LightColors = lightColorScheme(
    primary = LightNeuPalette.accent,
    secondary = LightNeuPalette.accentSecondary,
    background = LightNeuPalette.background,
    surface = LightNeuPalette.surface,
    onBackground = LightNeuPalette.textPrimary,
    onSurface = LightNeuPalette.textPrimary,
    error = LightNeuPalette.danger,
)

@Composable
fun PeeratVoipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val palette = if (darkTheme) DarkNeuPalette else LightNeuPalette
    val colorScheme = if (darkTheme) DarkColors else LightColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        val activity = view.context as? android.app.Activity
        activity?.window?.let { window ->
            window.statusBarColor = palette.background.toArgb()
            window.navigationBarColor = palette.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalNeuPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = PeeratTypography,
            content = content,
        )
    }
}
