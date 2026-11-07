package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class BlendCategory(val displayName: String) {
    NORMAL("Normal"),
    DARKEN("Darken"),
    LIGHTEN("Lighten"),
    CONTRAST("Contrast"),
    INVERSION("Inversion"),
    COMPONENT("Component")
}

@Serializable
enum class BlendMode(
    val displayName: String,
    val category: BlendCategory = BlendCategory.NORMAL,
    val glslFunction: String = "blendNormal"
) {
    // Normal
    NORMAL("Normal", BlendCategory.NORMAL, "blendNormal"),

    // Darken
    DARKEN("Darken", BlendCategory.DARKEN, "blendDarken"),
    MULTIPLY("Multiply", BlendCategory.DARKEN, "blendMultiply"),
    COLOR_BURN("Color Burn", BlendCategory.DARKEN, "blendColorBurn"),
    LINEAR_BURN("Linear Burn", BlendCategory.DARKEN, "blendLinearBurn"),

    // Lighten
    LIGHTEN("Lighten", BlendCategory.LIGHTEN, "blendLighten"),
    SCREEN("Screen", BlendCategory.LIGHTEN, "blendScreen"),
    COLOR_DODGE("Color Dodge", BlendCategory.LIGHTEN, "blendColorDodge"),
    LINEAR_DODGE("Linear Dodge (Add)", BlendCategory.LIGHTEN, "blendLinearDodge"),

    // Contrast
    OVERLAY("Overlay", BlendCategory.CONTRAST, "blendOverlay"),
    SOFT_LIGHT("Soft Light", BlendCategory.CONTRAST, "blendSoftLight"),
    HARD_LIGHT("Hard Light", BlendCategory.CONTRAST, "blendHardLight"),
    VIVID_LIGHT("Vivid Light", BlendCategory.CONTRAST, "blendVividLight"),
    LINEAR_LIGHT("Linear Light", BlendCategory.CONTRAST, "blendLinearLight"),
    PIN_LIGHT("Pin Light", BlendCategory.CONTRAST, "blendPinLight"),

    // Inversion
    DIFFERENCE("Difference", BlendCategory.INVERSION, "blendDifference"),
    EXCLUSION("Exclusion", BlendCategory.INVERSION, "blendExclusion"),
    SUBTRACT("Subtract", BlendCategory.INVERSION, "blendSubtract"),
    DIVIDE("Divide", BlendCategory.INVERSION, "blendDivide"),

    // Component
    HUE("Hue", BlendCategory.COMPONENT, "blendHue"),
    SATURATION("Saturation", BlendCategory.COMPONENT, "blendSaturation"),
    COLOR("Color", BlendCategory.COMPONENT, "blendColor"),
    LUMINOSITY("Luminosity", BlendCategory.COMPONENT, "blendLuminosity");

    companion object {
        fun byCategory(category: BlendCategory): List<BlendMode> =
            entries.filter { it.category == category }
    }
}

@Serializable
enum class MaskMode(val displayName: String) {
    NONE("No Mask"),
    IN_MASK("In-Mask (Show inside)"),
    OUT_MASK("Out-Mask (Show outside)")
}

@Serializable
enum class MaskType(val displayName: String) {
    NONE("None"),
    ALPHA("Alpha"),
    LUMINANCE("Luminance"),
    VECTOR("Vector")
}
