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
        val candidateDir = if (srcDir.exists()) srcDir else File("app/src/main/java/com/motionssalt/saltygraphics")
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
        })

        assertTrue(
            "Forbidden raw Material controls found! Use shared controls (SaltySwitch, ToggleControl, RulerSlider, ProgressSliderControl) from ui.controls instead:\n" +
                    violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun testScrollControlsHaveZeroHapticCalls() {
        val srcDir = File("src/main/java/com/motionssalt/saltygraphics/ui/controls")
        val candidateDir = if (srcDir.exists()) srcDir else File("app/src/main/java/com/motionssalt/saltygraphics/ui/controls")
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
        // Register a dynamic custom effect with all standard control types
        val customEffect = EffectDefinition(
            id = "custom_lens_blur",
            name = "Lens Blur Pro",
            category = EffectCategory.BLUR,
            description = "Dynamically registered professional lens blur effect",
            controls = listOf(
                ControlDefinition.Slider(
                    id = "radius",
                    label = "Blur Radius",
                    defaultValue = 15f,
                    minValue = 0f,
                    maxValue = 100f,
                    step = 0.5f,
                    unit = "px",
                    sliderStyle = SliderStyle.RULER
                ),
                ControlDefinition.Toggle(
                    id = "bokeh",
                    label = "Enable Bokeh",
                    defaultValue = true
                ),
                ControlDefinition.Color(
                    id = "tint",
                    label = "Aperture Tint",
                    defaultValue = 0x80FFFFFFL
                ),
                ControlDefinition.Dropdown(
                    id = "shape",
                    label = "Diaphragm Shape",
                    options = listOf("Circular", "Hexagonal", "Octagonal"),
                    defaultValue = "Hexagonal"
                )
            )
        )

        // Verify control properties and instantiation
        assertEquals("custom_lens_blur", customEffect.id)
        assertEquals(4, customEffect.controls.size)

        val slider = customEffect.controls[0] as ControlDefinition.Slider
        assertEquals("Blur Radius", slider.label)
        assertEquals(SliderStyle.RULER, slider.sliderStyle)

        val toggle = customEffect.controls[1] as ControlDefinition.Toggle
        assertEquals("Enable Bokeh", toggle.label)
        assertTrue(toggle.defaultValue)

        val color = customEffect.controls[2] as ControlDefinition.Color
        assertEquals(0x80FFFFFFL, color.defaultValue)

        val dropdown = customEffect.controls[3] as ControlDefinition.Dropdown
        assertEquals("Hexagonal", dropdown.defaultValue)
        assertEquals(3, dropdown.options.size)

        // Instantiate and test parameter mutation
        val instance = customEffect.instantiate()
        assertEquals(15f, instance.parameters["radius"])
        assertEquals(true, instance.parameters["bokeh"])
        assertEquals(0x80FFFFFFL, instance.parameters["tint"])
        assertEquals("Hexagonal", instance.parameters["shape"])

        val updatedInstance = instance.withParameter("radius", 45f)
            .withParameter("bokeh", false)
            .withParameter("shape", "Octagonal")

        assertEquals(45f, updatedInstance.parameters["radius"])
        assertEquals(false, updatedInstance.parameters["bokeh"])
        assertEquals("Octagonal", updatedInstance.parameters["shape"])
    }
}
