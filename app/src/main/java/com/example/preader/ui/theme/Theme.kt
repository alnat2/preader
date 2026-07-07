package com.example.preader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkActionPrimary,
    primaryContainer = DarkOnSurfaceAction,
    error = DarkActionDanger,
    errorContainer = DarkOnSurfaceDanger,
    background = DarkBgApp,
    surface = DarkSurface,
    surfaceVariant = DarkSurface,
    onBackground = DarkTextPrimary,
    onSurface = DarkTextPrimary,
    onSurfaceVariant = DarkTextSecondary,
    outlineVariant = DarkBorder,
    inverseSurface = DarkInverseSurface
)

private val LightColorScheme = lightColorScheme(
    primary = LightActionPrimary,
    primaryContainer = LightOnSurfaceAction,
    error = LightActionDanger,
    errorContainer = LightOnSurfaceDanger,
    background = LightBgApp,
    surface = LightSurface,
    surfaceVariant = LightSurface,
    onBackground = LightTextPrimary,
    onSurface = LightTextPrimary,
    onSurfaceVariant = LightTextSecondary,
    outlineVariant = LightBorder,
    inverseSurface = LightInverseSurface
)

@Composable
fun PreaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}