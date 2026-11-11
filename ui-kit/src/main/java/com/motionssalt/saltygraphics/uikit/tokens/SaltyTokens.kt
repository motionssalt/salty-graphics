package com.motionssalt.saltygraphics.uikit.tokens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Unified design tokens for Salty Graphics.
 * Feature code must NEVER use literal dp, sp, or color values directly.
 * All UI references these tokens via SaltyTheme or SaltyTokens.
 */
object SaltySpacing {
    val none: Dp = 0.dp
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    // Dedicated semantic spacing
    val panelPadding: Dp = 16.dp
    val sectionSpacing: Dp = 12.dp
    val rowSpacing: Dp = 8.dp
    val itemSpacing: Dp = 8.dp
    val toolGridSpacing: Dp = 8.dp
    val controlHeight: Dp = 40.dp
    val touchTargetMin: Dp = 48.dp
    val breadcrumbButton: Dp = 28.dp
}

object SaltyShapes {
    val none: Shape = RoundedCornerShape(0.dp)
    val xs: Shape = RoundedCornerShape(4.dp)
    val sm: Shape = RoundedCornerShape(6.dp)
    val md: Shape = RoundedCornerShape(8.dp)
    val lg: Shape = RoundedCornerShape(12.dp)
    val xl: Shape = RoundedCornerShape(16.dp)
    val pill: Shape = RoundedCornerShape(50)

    val button: Shape = RoundedCornerShape(8.dp)
    val card: Shape = RoundedCornerShape(12.dp)
    val tile: Shape = RoundedCornerShape(8.dp)
    val dialog: Shape = RoundedCornerShape(16.dp)
    val panelHeader: Shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
}

object SaltyTypography {
    val titleLarge: TextStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 26.sp
    )
    val titleMedium: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 22.sp
    )
    val titleSmall: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeight = 20.sp
    )
    val bodyLarge: TextStyle = TextStyle(
        fontSize = 16.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 22.sp
    )
    val bodyMedium: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 20.sp
    )
    val bodySmall: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        lineHeight = 16.sp
    )
    val labelLarge: TextStyle = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp
    )
    val labelMedium: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 16.sp
    )
    val labelSmall: TextStyle = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 14.sp
    )
    val mono: TextStyle = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
        lineHeight = 16.sp
    )
}

data class SaltyColorRoles(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val onError: Color,
    val trackOn: Color,
    val trackOff: Color,
    val thumbOn: Color,
    val thumbOff: Color,
    val canvasDefault: Color = Color(0xFF808080),
    val snapGuideSmart: Color = Color(0xFFFF2222),
    val snapGuideGrid: Color = Color(0xFF00E5FF),
    val selectionOutline: Color = Color(0xFF2979FF),
    val selectionHandle: Color = Color(0xFFFFFFFF),
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color
)

object SaltyThemeDefaults {
    fun darkRoles(): SaltyColorRoles = SaltyColorRoles(
        primary = Color(0xFFFF7A00),
        onPrimary = Color(0xFF000000),
        secondary = Color(0xFFFF9E40),
        onSecondary = Color(0xFF000000),
        accent = Color(0xFFFF7A00),
        onAccent = Color(0xFF000000),
        background = Color(0xFF121212),
        onBackground = Color(0xFFEDEDED),
        surface = Color(0xFF1E1E1E),
        onSurface = Color(0xFFE0E0E0),
        surfaceVariant = Color(0xFF2A2A2A),
        onSurfaceVariant = Color(0xFFAAAAAA),
        outline = Color(0xFF3E3E3E),
        outlineVariant = Color(0xFF2C2C2C),
        error = Color(0xFFCF6679),
        onError = Color(0xFF000000),
        trackOn = Color(0xFFFF7A00),
        trackOff = Color(0xFF383838),
        thumbOn = Color(0xFFFFFFFF),
        thumbOff = Color(0xFFB0B0B0),
        textPrimary = Color(0xFFEDEDED),
        textSecondary = Color(0xFFAAAAAA),
        textDisabled = Color(0xFF666666)
    )

    fun lightRoles(): SaltyColorRoles = SaltyColorRoles(
        primary = Color(0xFFE65100),
        onPrimary = Color(0xFFFFFFFF),
        secondary = Color(0xFFF57C00),
        onSecondary = Color(0xFFFFFFFF),
        accent = Color(0xFFE65100),
        onAccent = Color(0xFFFFFFFF),
        background = Color(0xFFF7F7F7),
        onBackground = Color(0xFF1A1A1A),
        surface = Color(0xFFFFFFFF),
        onSurface = Color(0xFF202020),
        surfaceVariant = Color(0xFFEEEEEE),
        onSurfaceVariant = Color(0xFF606060),
        outline = Color(0xFFD0D0D0),
        outlineVariant = Color(0xFFE5E5E5),
        error = Color(0xFFB00020),
        onError = Color(0xFFFFFFFF),
        trackOn = Color(0xFFE65100),
        trackOff = Color(0xFFD6D6D6),
        thumbOn = Color(0xFFFFFFFF),
        thumbOff = Color(0xFF707070),
        textPrimary = Color(0xFF1A1A1A),
        textSecondary = Color(0xFF606060),
        textDisabled = Color(0xFF9E9E9E)
    )
}

val LocalSaltyColorRoles = staticCompositionLocalOf { SaltyThemeDefaults.darkRoles() }

object SaltyTokens {
    val spacing: SaltySpacing = SaltySpacing
    val shapes: SaltyShapes = SaltyShapes
    val typography: SaltyTypography = SaltyTypography

    val colors: SaltyColorRoles
        @Composable
        @ReadOnlyComposable
        get() = LocalSaltyColorRoles.current
}
