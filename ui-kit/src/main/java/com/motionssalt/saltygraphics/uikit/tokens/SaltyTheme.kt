package com.motionssalt.saltygraphics.uikit.tokens

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val LocalUiScale = compositionLocalOf { 1.0f }

object SaltyPalette {
    val DarkBackground = Color(0xFF0C0E14)
    val SurfaceDark = Color(0xFF141721)
    val SurfaceVariantDark = Color(0xFF1D2230)
    val OutlineDark = Color(0xFF2E364A)
    val Cyan = Color(0xFF00E5FF)
    val CyanGlow = Color(0xFF6EFFFF)
    val Amber = Color(0xFFFFB300)
    val ElectricBlue = Color(0xFF2979FF)
    val Error = Color(0xFFFF5252)
    val TextPrimary = Color(0xFFF0F4FC)
    val TextSecondary = Color(0xFF98A4BC)
    val TextTertiary = Color(0xFF64728C)
}

fun saltySafeColor(argb: Long): Color = Color(argb.toInt())

val SaltyMaterialTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

private val DarkColorScheme = darkColorScheme(
    primary = SaltyPalette.Cyan,
    onPrimary = SaltyPalette.DarkBackground,
    primaryContainer = SaltyPalette.SurfaceVariantDark,
    onPrimaryContainer = SaltyPalette.CyanGlow,
    secondary = SaltyPalette.Amber,
    onSecondary = SaltyPalette.DarkBackground,
    background = SaltyPalette.DarkBackground,
    onBackground = SaltyPalette.TextPrimary,
    surface = SaltyPalette.SurfaceDark,
    onSurface = SaltyPalette.TextPrimary,
    surfaceVariant = SaltyPalette.SurfaceVariantDark,
    onSurfaceVariant = SaltyPalette.TextSecondary,
    outline = SaltyPalette.OutlineDark,
    error = SaltyPalette.Error
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
        DisposableEffect(view, darkTheme) {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) window.isNavigationBarContrastEnforced = false
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
            onDispose { }
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
        LocalSaltyColorRoles provides SaltyThemeDefaults.materialRoles(colorScheme),
        LocalUiScale provides effectiveScale,
        LocalDensity provides scaledDensity
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SaltyMaterialTypography,
            content = content
        )
    }
}
