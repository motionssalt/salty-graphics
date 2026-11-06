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
enum class TextBoxMode(val displayName: String) {
    FREE("Free"),
    PARAGRAPH("Paragraph"),
    FIXED_SIZE("Fixed size"),
    SHRINK_TO_FIT("Shrink to fit")
}

@Serializable
data class TextLayerData(
    val text: String = "Salty Graphics",
    val fontFamily: String = "sans-serif",
    val fontStyleName: String = "Regular",
    val customFontAssetPath: String? = null,
    val fontSize: Float = 64f,
    val color: Long = 0xFFFFFFFFL,
    val justification: TextJustification = TextJustification.CENTER,
    val alignment: TextAlignment = TextAlignment.MIDDLE,
    val letterSpacing: Float = 0f, // in ‰ em
    val lineSpacing: Float = 120f, // in %
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val hasOutline: Boolean = false,
    val outlineColor: Long = 0xFF000000L,
    val outlineWidth: Float = 2f,
    val boxMode: TextBoxMode = TextBoxMode.FREE,
    val boxWidth: Float = 400f,
    val boxHeight: Float = 200f,
    val followPathLayerId: String? = null,
    val pathOffsetPercent: Float = 0f // 0% to 100% position along path
)
