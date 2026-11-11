package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.engine.effect.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Architectural and extensibility test suite for V4 and V5:
 * 1. Enforces that no app code outside ui/controls directly uses raw Material Switch or Slider
 * 2. Tests dynamic registration and resolution of custom controls
 * 3. Enforces that scroll/scrub controls do not contain haptic feedback calls
 */
class ControlEngineArchitectureTest {

    @Test
    fun testNoRawMaterialSwitchOrSliderOutsideControlLibrary() {
        // Locate app source directory
        val srcDir = File("src/main/java/com/motionssalt/saltygraphics")
        val candidateDir = if (srcDir.exists()) srcDir else File("ui-kit/src/main/java/com/motionssalt/saltygraphics")
        assertTrue("Source directory must exist: ${candidateDir.absolutePath}", candidateDir.exists())

        val violations = mutableListOf<String>()
        candidateDir.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val relativePath = file.relativeTo(candidateDir).path
            // Allow the library implementations themselves
            if (relativePath.contains("ui/controls/SaltySwitch.kt") ||
                relativePath.contains("ui/controls/ProgressSliderControl.kt")
            ) {
                return@forEach
            }

            val lines = file.readLines()
            lines.forEachIndexed { lineIdx, line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                    return@forEachIndexed
                }
                // Check raw Switch import or usage
                if (trimmed.contains("import androidx.compose.material3.Switch") ||
                    trimmed.matches(Regex(".*\\bSwitch\\s*\\(.*"))
                ) {
                    violations.add("$relativePath:${lineIdx + 1}: Found raw Switch usage ($trimmed)")
                }
                // Check raw Slider / RangeSlider import or usage
                if (trimmed.contains("import androidx.compose.material3.Slider") ||
                    trimmed.contains("import androidx.compose.material3.RangeSlider") ||
                    trimmed.matches(Regex(".*\\b(Slider|RangeSlider)\\s*\\(.*"))
                ) {
                    violations.add("$relativePath:${lineIdx + 1}: Found raw Slider usage ($trimmed)")
                }
            }
        }

        assertTrue(
            "Forbidden raw Material controls found! Use shared controls (SaltySwitch, ToggleControl, RulerSlider, ProgressSliderControl) from ui.controls instead:\n" +
                    violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun testScrollControlsHaveZeroHapticCalls() {
        val srcDir = File("src/main/java/com/motionssalt/saltygraphics/ui/controls")
        val candidateDir = if (srcDir.exists()) srcDir else File("ui-kit/src/main/java/com/motionssalt/saltygraphics/ui/controls")
        assertTrue("Controls directory must exist", candidateDir.exists())

        val rulerFile = File(candidateDir, "RulerSlider.kt")
        assertTrue("RulerSlider.kt must exist", rulerFile.exists())

        val rulerContent = rulerFile.readText()
        assertFalse(
            "RulerSlider must NOT contain any haptic feedback calls (V5)",
            rulerContent.contains("performHapticFeedback")
        )
        assertFalse(
            "RulerSlider must NOT import HapticFeedbackConstants (V5)",
            rulerContent.contains("HapticFeedbackConstants")
        )
    }

    @Test
    fun testDynamicControlRegistrationAndExtensibility() {
        // Register a dynamic custom effect with standard control types
        val customEffect = EffectDefinition(
            id = "custom_lens_blur",
            name = "Lens Blur Pro",
            description = "Dynamically registered professional lens blur effect",
            glslShader = "// GLSL shader stub",
            controls = listOf(
                ControlDefinition(
                    id = "radius",
                    label = "Blur Radius",
                    type = ControlType.SLIDER,
                    defaultValue = 15f,
                    minValue = 0f,
                    maxValue = 100f,
                    step = 0.5f,
                    unit = "px",
                    sliderStyle = SliderStyle.RULER
                ),
                ControlDefinition(
                    id = "bokeh",
                    label = "Enable Bokeh",
                    type = ControlType.TOGGLE,
                    defaultToggle = true
                ),
                ControlDefinition(
                    id = "tint",
                    label = "Aperture Tint",
                    type = ControlType.COLOR_PICKER,
                    defaultColor = 0x80FFFFFFL
                ),
                ControlDefinition(
                    id = "shape",
                    label = "Diaphragm Shape",
                    type = ControlType.DROPDOWN,
                    options = listOf("Circular", "Hexagonal", "Octagonal")
                )
            )
        )

        // Register and verify retrieval from registry
        EffectRegistry.registerEffect(customEffect)
        val retrieved = EffectRegistry.getEffectDefinition("custom_lens_blur")
        assertNotNull(retrieved)
        assertEquals("Lens Blur Pro", retrieved?.name)
        assertEquals(4, retrieved?.controls?.size)

        val slider = retrieved!!.controls[0]
        assertEquals("Blur Radius", slider.label)
        assertEquals(ControlType.SLIDER, slider.type)
        assertEquals(SliderStyle.RULER, slider.sliderStyle)

        val toggle = retrieved.controls[1]
        assertEquals("Enable Bokeh", toggle.label)
        assertEquals(ControlType.TOGGLE, toggle.type)
        assertTrue(toggle.defaultToggle)

        val color = retrieved.controls[2]
        assertEquals("Aperture Tint", color.label)
        assertEquals(ControlType.COLOR_PICKER, color.type)
        assertEquals(0x80FFFFFFL, color.defaultColor)

        val dropdown = retrieved.controls[3]
        assertEquals("Diaphragm Shape", dropdown.label)
        assertEquals(ControlType.DROPDOWN, dropdown.type)
        assertEquals(3, dropdown.options.size)

        // Instantiate EffectInstance with parameters and verify mutations
        val instance = EffectInstance(
            effectId = customEffect.id,
            floatParams = mapOf("radius" to 15f),
            boolParams = mapOf("bokeh" to true),
            colorParams = mapOf("tint" to 0x80FFFFFFL),
            stringParams = mapOf("shape" to "Hexagonal")
        )
        assertEquals(15f, instance.getFloat("radius"))
        assertTrue(instance.getBool("bokeh"))
        assertEquals(0x80FFFFFFL, instance.getColor("tint"))
        assertEquals("Hexagonal", instance.getString("shape"))

        val updatedInstance = instance.copy(
            floatParams = instance.floatParams + ("radius" to 45f),
            boolParams = instance.boolParams + ("bokeh" to false),
            stringParams = instance.stringParams + ("shape" to "Octagonal")
        )
        assertEquals(45f, updatedInstance.getFloat("radius"))
        assertFalse(updatedInstance.getBool("bokeh"))
        assertEquals("Octagonal", updatedInstance.getString("shape"))
    }
}
