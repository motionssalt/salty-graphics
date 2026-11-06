package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class TextJustification(val displayName: String) {
    LEFT("Left"),
    CENTER("Center"),
    RIGHT("Right"),
    JUSTIFY("Justify")
}

@Serializable
enum class TextAlignment(val displayName: String) {
    TOP("Top"),
    MIDDLE("Middle"),
    BOTTOM("Bottom")
}

@Serializable
data class TextLayerData(
    val text: String = "Salty Graphics",
    val fontFamily: String = "sans-serif",
    val customFontAssetPath: String? = null,
    val fontSize: Float = 64f,
    val color: Long = 0xFFFFFFFF,
    val justification: TextJustification = TextJustification.CENTER,
    val alignment: TextAlignment = TextAlignment.MIDDLE,
    val letterSpacing: Float = 0f,
    val lineSpacing: Float = 1.2f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val followPathLayerId: String? = null,
    val pathOffsetPercent: Float = 0f // 0% to 100% position along path
)
