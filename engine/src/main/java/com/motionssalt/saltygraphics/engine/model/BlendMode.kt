package com.motionssalt.saltygraphics.engine.model

import kotlinx.serialization.Serializable

@Serializable
enum class BlendMode(val displayName: String, val glslFunction: String) {
    NORMAL("Normal", "blendNormal"),
    MULTIPLY("Multiply", "blendMultiply"),
    SCREEN("Screen", "blendScreen"),
    OVERLAY("Overlay", "blendOverlay"),
    DARKEN("Darken", "blendDarken"),
    LIGHTEN("Lighten", "blendLighten"),
    COLOR_DODGE("Color Dodge", "blendColorDodge"),
    COLOR_BURN("Color Burn", "blendColorBurn"),
    SOFT_LIGHT("Soft Light", "blendSoftLight"),
    HARD_LIGHT("Hard Light", "blendHardLight"),
    DIFFERENCE("Difference", "blendDifference"),
    EXCLUSION("Exclusion", "blendExclusion"),
    HUE("Hue", "blendHue"),
    SATURATION("Saturation", "blendSaturation"),
    COLOR("Color", "blendColor"),
    LUMINOSITY("Luminosity", "blendLuminosity")
}

@Serializable
enum class MaskMode(val displayName: String) {
    NONE("No Mask"),
    IN_MASK("In-Mask (Show inside)"),
    OUT_MASK("Out-Mask (Show outside)")
}
