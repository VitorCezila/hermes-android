package com.cezila.hermes.core.ui.theme

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
    primary = Clay,
    onPrimary = Paper,
    primaryContainer = ClayDark,
    onPrimaryContainer = Paper,
    secondary = ClayLight,
    onSecondary = Paper,
    secondaryContainer = ClayTintLight,
    onSecondaryContainer = ClayDark,
    background = Paper,
    onBackground = Charcoal,
    surface = Paper,
    onSurface = Charcoal,
    surfaceVariant = CharcoalLight,
    onSurfaceVariant = Charcoal,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = PaperContainerLow,
    surfaceContainer = PaperContainer,
    surfaceContainerHigh = PaperContainerHigh,
    surfaceContainerHighest = PaperContainerHighest,
    scrim = Charcoal,
    error = ErrorRed,
    onError = Paper,
    errorContainer = ErrorRedLight,
    onErrorContainer = ErrorRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = ClayLight,
    onPrimary = Charcoal,
    primaryContainer = Clay,
    onPrimaryContainer = Paper,
    secondary = ClayLight,
    onSecondary = Charcoal,
    secondaryContainer = ClayTintDark,
    onSecondaryContainer = ClayTintLight,
    background = Charcoal,
    onBackground = Paper,
    surface = PaperDark,
    onSurface = Paper,
    surfaceVariant = PaperDark,
    onSurfaceVariant = CharcoalLight,
    surfaceContainerLowest = CharcoalContainerLowest,
    surfaceContainerLow = CharcoalContainerLow,
    surfaceContainer = CharcoalContainer,
    surfaceContainerHigh = CharcoalContainerHigh,
    surfaceContainerHighest = CharcoalContainerHighest,
    scrim = Charcoal,
    error = ErrorRedLight,
    onError = ErrorRed,
    errorContainer = ErrorRed,
    onErrorContainer = ErrorRedLight,
)

@Composable
fun HermesAndroidTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
