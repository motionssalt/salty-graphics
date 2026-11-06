package com.motionssalt.saltygraphics.engine.effect

import kotlinx.serialization.Serializable

@Serializable
enum class SliderStyle {
    RULER,
    PROGRESS
}

@Serializable
enum class ControlType {
    TOGGLE,
    SLIDER,
    DROPDOWN,
    COLOR_PICKER,
    PROGRESS_SLIDER,
    LAYER_SELECTOR,
    SEGMENTED_CHIPS,
    TEXT_FIELD
}

@Serializable
data class ControlDefinition(
    val id: String,
    val label: String,
    val type: ControlType,
    val defaultValue: Float = 0f,
    val minValue: Float = 0f,
    val maxValue: Float = 1f,
    val step: Float = 0.01f,
    val unit: String = "",
    val sliderStyle: SliderStyle = SliderStyle.RULER,
    val options: List<String> = emptyList(), // For DROPDOWN and SEGMENTED_CHIPS
    val defaultColor: Long = 0xFFFFFFFFL,   // For COLOR_PICKER
    val defaultToggle: Boolean = false,     // For TOGGLE
    val defaultText: String = ""            // For LAYER_SELECTOR and TEXT_FIELD
)

@Serializable
data class EffectDefinition(
    val id: String,
    val name: String,
    val description: String = "",
    val glslShader: String,
    val controls: List<ControlDefinition>
)

@Serializable
data class EffectInstance(
    val effectId: String,
    val isEnabled: Boolean = true,
    val floatParams: Map<String, Float> = emptyMap(),
    val boolParams: Map<String, Boolean> = emptyMap(),
    val colorParams: Map<String, Long> = emptyMap(),
    val stringParams: Map<String, String> = emptyMap() // For layer selector or dropdown option
) {
    fun getFloat(id: String, default: Float = 0f): Float = floatParams[id] ?: default
    fun getBool(id: String, default: Boolean = false): Boolean = boolParams[id] ?: default
    fun getColor(id: String, default: Long = 0xFFFFFFFF): Long = colorParams[id] ?: default
    fun getString(id: String, default: String = ""): String = stringParams[id] ?: default
}

/**
 * Registry of effect definitions.
 * In production/beta, this holds zero shipped effects, but exposes the full plumbing
 * and a test definition used for verification.
 */
object EffectRegistry {
    private val definitions = mutableMapOf<String, EffectDefinition>()

    /**
     * Test effect definition exercising every single control type.
     */
    val TEST_EFFECT = EffectDefinition(
        id = "test_all_controls",
        name = "Test Harness Effect",
        description = "Exercises all 6 control types to verify automatic UI generation & shader uniform binding.",
        glslShader = """
            #version 300 es
            precision highp float;
            in vec2 vTexCoord;
            out vec4 fragColor;
            
            uniform sampler2D uTexture;
            uniform sampler2D uBackdrop;
            uniform vec2 uCanvasSize;
            uniform float uTime;
            
            // Effect controls
            uniform int uToggle;
            uniform float uIntensitySlider;
            uniform int uModeDropdown;
            uniform vec4 uTintPicker;
            uniform float uProgress;
            
            void main() {
                vec4 src = texture(uTexture, vTexCoord);
                vec4 backdrop = texture(uBackdrop, vTexCoord);
                
                if (uToggle == 0) {
                    fragColor = src;
                    return;
                }
                
                vec4 tinted = mix(src, uTintPicker, uIntensitySlider);
                fragColor = mix(tinted, backdrop, uProgress * 0.5);
            }
        """.trimIndent(),
        controls = listOf(
            ControlDefinition(
                id = "toggle_active",
                label = "Enable Processing",
                type = ControlType.TOGGLE,
                defaultToggle = true
            ),
            ControlDefinition(
                id = "intensity_slider",
                label = "Intensity",
                type = ControlType.SLIDER,
                defaultValue = 0.5f,
                minValue = 0f,
                maxValue = 1f,
                step = 0.01f,
                unit = "%",
                sliderStyle = SliderStyle.RULER
            ),
            ControlDefinition(
                id = "mode_dropdown",
                label = "Blend Technique",
                type = ControlType.DROPDOWN,
                options = listOf("Standard", "Add", "Multiply", "Subtractive")
            ),
            ControlDefinition(
                id = "tint_color",
                label = "Tint Color",
                type = ControlType.COLOR_PICKER,
                defaultColor = 0xFF00E5FFL
            ),
            ControlDefinition(
                id = "progress_sweep",
                label = "Sweep Progress",
                type = ControlType.PROGRESS_SLIDER,
                defaultValue = 0.25f,
                minValue = 0f,
                maxValue = 1f,
                unit = "%",
                sliderStyle = SliderStyle.PROGRESS
            ),
            ControlDefinition(
                id = "reference_layer",
                label = "Masking Layer",
                type = ControlType.LAYER_SELECTOR,
                defaultText = ""
            )
        )
    )

    init {
        // Register test effect internally
        definitions[TEST_EFFECT.id] = TEST_EFFECT
    }

    /**
     * For Beta release: returns empty list by default (Section 4.2: ship zero real effects).
     * If includeDebug = true, includes the test harness effect.
     */
    fun getAvailableEffects(includeDebug: Boolean = false): List<EffectDefinition> {
        return if (includeDebug) definitions.values.toList() else emptyList()
    }

    fun getEffectDefinition(id: String): EffectDefinition? = definitions[id]

    fun registerEffect(definition: EffectDefinition) {
        definitions[definition.id] = definition
    }
}
