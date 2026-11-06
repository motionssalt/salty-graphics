package com.motionssalt.saltygraphics.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
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
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> DarkColorScheme
    }

    val currentDensity = LocalContext.current.resources.displayMetrics.density
    val scaledDensity = Density(
        density = currentDensity * uiScale,
        fontScale = uiScale
    )

    CompositionLocalProvider(
        LocalUiScale provides uiScale,
        androidx.compose.ui.platform.LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SaltyTypography,
            content = content
        )
    }
}
