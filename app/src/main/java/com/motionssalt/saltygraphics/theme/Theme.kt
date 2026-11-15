package com.motionssalt.saltygraphics.theme

import androidx.compose.runtime.Composable

@Composable
fun SaltyGraphicsTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    uiScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    com.motionssalt.saltygraphics.uikit.tokens.SaltyGraphicsTheme(
        darkTheme = darkTheme,
        dynamicColor = dynamicColor,
        uiScale = uiScale,
        content = content
    )
}
