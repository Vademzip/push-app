package com.pushapp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary              = AppAccent,
    onPrimary            = AppBackground,
    primaryContainer     = AppAccentDim,
    onPrimaryContainer   = AppAccent,
    secondary            = AppSurfaceBright,
    onSecondary          = AppOnBackground,
    secondaryContainer   = AppSurfaceVariant,
    onSecondaryContainer = AppOnBackground,
    tertiary             = AppAccent,
    onTertiary           = AppBackground,
    background           = AppBackground,
    onBackground         = AppOnBackground,
    surface              = AppSurface,
    onSurface            = AppOnSurface,
    surfaceVariant       = AppSurfaceVariant,
    onSurfaceVariant     = AppOnSurfaceVar,
    error                = AppError,
    onError              = AppBackground,
    outline              = AppSurfaceBright,
    outlineVariant       = AppSurfaceVariant,
)

@Composable
fun PushAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
