package com.motionssalt.saltygraphics.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.core.view.WindowCompat

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

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EEFFD),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF785900),
    onSecondary = Color.White,
    background = Color(0xFFFBFDFD),
    onBackground = Color(0xFF191C1D),
    surface = Color(0xFFF8FAFA),
    onSurface = Color(0xFF191C1D),
    surfaceVariant = Color(0xFFDBE4E6),
    onSurfaceVariant = Color(0xFF3F484A),
    outline = Color(0xFF6F797B),
    error = Color(0xFFBA1A1A)
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
                if (darkTheme) DarkColorScheme else LightColorScheme
            }
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
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
