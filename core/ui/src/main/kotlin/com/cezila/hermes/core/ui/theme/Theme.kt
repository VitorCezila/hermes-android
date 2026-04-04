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
    background = Paper,
    onBackground = Charcoal,
    surface = Paper,
    onSurface = Charcoal,
    surfaceVariant = CharcoalLight,
    onSurfaceVariant = Charcoal,
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
    background = Charcoal,
    onBackground = Paper,
    surface = PaperDark,
    onSurface = Paper,
    surfaceVariant = PaperDark,
    onSurfaceVariant = CharcoalLight,
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
