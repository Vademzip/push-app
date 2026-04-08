package com.pushapp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun PushAppTheme(
    accent: Color = AppAccent,
    accentDim: Color = AppAccentDim,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary              = accent,
        onPrimary            = AppBackground,
        primaryContainer     = accentDim,
        onPrimaryContainer   = accent,
        secondary            = AppSurfaceBright,
        onSecondary          = AppOnBackground,
        secondaryContainer   = AppSurfaceVariant,
        onSecondaryContainer = AppOnBackground,
        tertiary             = accent,
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
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
