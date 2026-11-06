package com.motionssalt.saltygraphics.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

val LocalUiScale = compositionLocalOf { 1.0f }

private val DarkColorScheme = darkColorScheme(
    primary = SaltyCyan,
    onPrimary = SaltyDarkBackground,
    primaryContainer = SaltySurfaceVariantDark,
    onPrimaryContainer = SaltyCyanGlow,
    secondary = SaltyAmber,
    onSecondary = SaltyDarkBackground,
    background = SaltyDarkBackground,
    onBackground = SaltyTextPrimary,
    surface = SaltySurfaceDark,
    onSurface = SaltyTextPrimary,
    surfaceVariant = SaltySurfaceVariantDark,
    onSurfaceVariant = SaltyTextSecondary,
    outline = SaltyOutlineDark,
    error = SaltyError
)

@Composable
fun SaltyGraphicsTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    uiScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            try {
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } catch (_: Throwable) {
                DarkColorScheme
            }
        }
        else -> DarkColorScheme
    }

    val currentDensity = LocalDensity.current
    val effectiveScale = uiScale.coerceIn(0.5f, 2.5f)
    val scaledDensity = remember(currentDensity.density, currentDensity.fontScale, effectiveScale) {
        Density(
            density = currentDensity.density * effectiveScale,
            fontScale = currentDensity.fontScale * effectiveScale
        )
    }

    CompositionLocalProvider(
        LocalUiScale provides effectiveScale,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SaltyTypography,
            content = content
        )
    }
}
