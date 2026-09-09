package com.example.ui.theme

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = PosPrimary,
    onPrimary = PosOnPrimary,
    primaryContainer = PosPrimaryContainer,
    onPrimaryContainer = PosOnPrimaryContainer,
    secondary = PosSecondary,
    onSecondary = PosOnSecondary,
    secondaryContainer = PosSecondaryContainer,
    onSecondaryContainer = PosOnSecondaryContainer,
    tertiary = PosTertiary,
    tertiaryContainer = PosTertiaryContainer,
    onTertiaryContainer = PosOnTertiaryContainer,
    background = PosBackground,
    onBackground = PosOnBackground,
    surface = PosSurface,
    onSurface = PosOnSurface,
    surfaceVariant = PosSurfaceVariant,
    onSurfaceVariant = PosOnSurfaceVariant,
    outline = PosOutline,
    error = PosError,
    errorContainer = PosErrorContainer,
    onErrorContainer = PosOnErrorContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = PosPrimaryDark,
    onPrimary = PosOnPrimaryDark,
    primaryContainer = PosPrimaryContainerDark,
    onPrimaryContainer = PosOnPrimaryContainerDark,
    secondary = PosSecondaryDark,
    onSecondary = PosOnSecondaryDark,
    secondaryContainer = PosSecondaryContainerDark,
    onSecondaryContainer = PosOnSecondaryContainerDark,
    tertiary = PosTertiaryDark,
    onTertiary = PosOnTertiaryDark,
    tertiaryContainer = PosTertiaryContainerDark,
    onTertiaryContainer = PosOnTertiaryContainerDark,
    background = PosBackgroundDark,
    onBackground = PosOnBackgroundDark,
    surface = PosSurfaceDark,
    onSurface = PosOnSurfaceDark,
    surfaceVariant = PosSurfaceVariantDark,
    onSurfaceVariant = PosOnSurfaceVariantDark,
    outline = PosOutlineDark,
    error = PosErrorDark,
    errorContainer = PosErrorContainerDark,
    onErrorContainer = PosOnErrorContainerDark
)

@Composable
fun PosTerminalTheme(
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
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
