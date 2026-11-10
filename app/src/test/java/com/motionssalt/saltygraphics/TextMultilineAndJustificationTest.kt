package com.motionssalt.saltygraphics

import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.engine.render.TextLayoutEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class TextMultilineAndJustificationTest {

    @Test
    fun testExplicitLineBreaksPreserved() {
        val multilineText = "Line 1\nLine 2\nLine 3"
        val textData = TextLayerData(
            text = multilineText,
            fontSize = 40f,
            lineSpacing = 120f,
            boxMode = TextBoxMode.FREE
        )
        val paint = Paint().apply { textSize = 40f }
        val layout = TextLayoutEngine.computeLayout(textData, paint)

        assertEquals("Should preserve exactly 3 lines", 3, layout.lines.size)
        assertEquals("Line 1", layout.lines[0].text)
        assertEquals("Line 2", layout.lines[1].text)
        assertEquals("Line 3", layout.lines[2].text)
        assertTrue(layout.lines[0].isParagraphEnd)
        assertTrue(layout.lines[1].isParagraphEnd)
        assertTrue(layout.lines[2].isParagraphEnd)
    }

    @Test
    fun testWordWrapToBoxWidth() {
        val longSentence = "The quick brown fox jumps over the lazy dog repeatedly and smoothly"
        val textData = TextLayerData(
            text = longSentence,
            fontSize = 40f,
            boxWidth = 300f,
            boxMode = TextBoxMode.PARAGRAPH
        )
        val paint = Paint().apply { textSize = 40f }
        assertTrue("Native font measurement must be active", paint.measureText(longSentence) > textData.boxWidth)
        val layout = TextLayoutEngine.computeLayout(textData, paint)

        assertTrue("Should wrap into multiple lines when boxWidth is constrained", layout.lines.size > 1)
        for (i in 0 until layout.lines.size - 1) {
            assertFalse("Non-final wrapped line should not be paragraph end", layout.lines[i].isParagraphEnd)
        }
        assertTrue("Final wrapped line should be marked as paragraph end", layout.lines.last().isParagraphEnd)
    }

    @Test
    fun testEmptyLinesPreservedInMultiline() {
        val textWithBlank = "Paragraph 1\n\nParagraph 2"
        val textData = TextLayerData(
            text = textWithBlank,
            fontSize = 32f,
            boxMode = TextBoxMode.PARAGRAPH
        )
        val paint = Paint().apply { textSize = 32f }
        val layout = TextLayoutEngine.computeLayout(textData, paint)

        assertEquals(3, layout.lines.size)
        assertEquals("Paragraph 1", layout.lines[0].text)
        assertEquals("", layout.lines[1].text)
        assertEquals("Paragraph 2", layout.lines[2].text)
    }

    @Test
    fun testJustificationSpacingCalculation() {
        val text = "First Second Third"
        val textData = TextLayerData(
            text = text,
            fontSize = 30f,
            boxWidth = 600f,
            justification = TextJustification.JUSTIFY,
            boxMode = TextBoxMode.PARAGRAPH
        )
        val paint = Paint().apply { textSize = 30f }
        val layout = TextLayoutEngine.computeLayout(textData, paint)

        assertEquals(1, layout.lines.size)
        // Ensure total layout height accounts for font metrics and line spacing
        assertTrue(layout.totalHeight > 0f)
        assertTrue(layout.lineHeight > 0f)
    }
}
