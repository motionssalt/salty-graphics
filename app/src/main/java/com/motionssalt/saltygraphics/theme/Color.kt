package com.motionssalt.saltygraphics.theme

import androidx.compose.ui.graphics.Color
import com.motionssalt.saltygraphics.uikit.tokens.saltySafeColor

fun safeColor(argb: Long): Color = saltySafeColor(argb)
fun Long.toComposeColor(): Color = saltySafeColor(this)
