package com.motionssalt.saltygraphics.engine.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.motionssalt.saltygraphics.engine.model.*
import kotlin.math.max

/**
 * Robust Multiline and Justified Text Layout Engine for Salty Graphics.
 * Preserves explicit line breaks (\n), wraps text automatically to boxWidth,
 * and handles Left, Center, Right, and Justify alignment across lines.
 */
object TextLayoutEngine {

    data class TextLine(
        val text: String,
        val width: Float,
        val isParagraphEnd: Boolean
    )

    data class TextLayoutResult(
        val lines: List<TextLine>,
        val totalWidth: Float,
        val totalHeight: Float,
        val lineHeight: Float,
        val baselineOffset: Float
    )

    /**
     * Computes the line layout for textData using explicit newlines and automatic wrapping.
     */
    fun computeLayout(
        textData: TextLayerData,
        paint: Paint
    ): TextLayoutResult {
        val rawText = textData.text
        if (rawText.isEmpty()) {
            val fm = paint.fontMetrics
            val lh = (fm.descent - fm.ascent) * (textData.lineSpacing / 100f)
            return TextLayoutResult(
                lines = emptyList(),
                totalWidth = 0f,
                totalHeight = max(lh, 20f),
                lineHeight = lh,
                baselineOffset = -fm.ascent
            )
        }

        val fm = paint.fontMetrics
        val lineHeight = (fm.descent - fm.ascent) * (textData.lineSpacing / 100f)
        val baselineOffset = -fm.ascent

        val maxBoxW = when (textData.boxMode) {
            TextBoxMode.FREE -> Float.MAX_VALUE
            else -> max(50f, textData.boxWidth)
        }

        // 1. Split raw text by explicit line breaks (\r\n or \n), preserving blank lines
        val rawParagraphs = rawText.split("\r\n", "\n")
        val lines = mutableListOf<TextLine>()

        for (pIdx in rawParagraphs.indices) {
            val paragraph = rawParagraphs[pIdx]
            val isLastParagraph = (pIdx == rawParagraphs.size - 1)

            if (paragraph.isEmpty()) {
                // Preserved empty line
                lines.add(TextLine(text = "", width = 0f, isParagraphEnd = true))
                continue
            }

            // If FREE mode or text fits in maxBoxW, keep paragraph intact
            val fullWidth = paint.measureText(paragraph)
            if (fullWidth <= maxBoxW) {
                lines.add(TextLine(text = paragraph, width = fullWidth, isParagraphEnd = true))
                continue
            }

            // Otherwise, wrap words onto multiple lines
            val words = paragraph.split(" ")
            var currentLine = StringBuilder()
            var currentLineWidth = 0f

            for (wIdx in words.indices) {
                val word = words[wIdx]
                val wordWithSpace = if (currentLine.isEmpty()) word else " $word"
                val wordWidth = paint.measureText(wordWithSpace)

                if (currentLine.isNotEmpty() && (currentLineWidth + wordWidth > maxBoxW)) {
                    // Line wrapped
                    lines.add(TextLine(text = currentLine.toString(), width = currentLineWidth, isParagraphEnd = false))
                    currentLine = StringBuilder(word)
                    currentLineWidth = paint.measureText(word)
                } else {
                    currentLine.append(wordWithSpace)
                    currentLineWidth += wordWidth
                }
            }

            if (currentLine.isNotEmpty()) {
                lines.add(TextLine(text = currentLine.toString(), width = currentLineWidth, isParagraphEnd = true))
            }
        }

        val totalWidth = if (lines.isEmpty()) 0f else lines.maxOf { it.width }
        val totalHeight = max(lines.size * lineHeight, lineHeight)

        return TextLayoutResult(
            lines = lines,
            totalWidth = totalWidth,
            totalHeight = totalHeight,
            lineHeight = lineHeight,
            baselineOffset = baselineOffset
        )
    }

    /**
     * Draws the computed multiline text onto an Android Canvas with full justification support.
     */
    fun drawTextLayout(
        canvas: Canvas,
        textData: TextLayerData,
        textPaint: Paint,
        strokePaint: Paint? = null,
        shadowPaint: Paint? = null,
        path: Path? = null
    ) {
        // If following path, use drawTextOnPath
        if (path != null && textData.followPathLayerId != null) {
            val pathLen = android.graphics.PathMeasure(path, false).length
            val hOffset = pathLen * textData.pathOffsetPercent
            if (shadowPaint != null) {
                canvas.drawTextOnPath(textData.text, path, hOffset + textData.letterSpacing, 0f, shadowPaint)
            }
            canvas.drawTextOnPath(textData.text, path, hOffset, 0f, textPaint)
            if (strokePaint != null) {
                canvas.drawTextOnPath(textData.text, path, hOffset, 0f, strokePaint)
            }
            return
        }

        val layout = computeLayout(textData, textPaint)
        if (layout.lines.isEmpty()) return

        val boxW = when (textData.boxMode) {
            TextBoxMode.FREE -> layout.totalWidth
            else -> max(layout.totalWidth, textData.boxWidth)
        }
        val boxH = when (textData.boxMode) {
            TextBoxMode.FREE -> layout.totalHeight
            else -> max(layout.totalHeight, textData.boxHeight)
        }

        // Vertical Alignment inside boxHeight
        val startY = when (textData.alignment) {
            TextAlignment.TOP -> -boxH / 2f + layout.baselineOffset
            TextAlignment.BOTTOM -> boxH / 2f - layout.totalHeight + layout.baselineOffset
            TextAlignment.MIDDLE -> -layout.totalHeight / 2f + layout.baselineOffset
        }

        for (i in layout.lines.indices) {
            val line = layout.lines[i]
            val y = startY + (i * layout.lineHeight)

            when (textData.justification) {
                TextJustification.LEFT -> {
                    val x = -boxW / 2f
                    textPaint.textAlign = Paint.Align.LEFT
                    strokePaint?.textAlign = Paint.Align.LEFT
                    shadowPaint?.textAlign = Paint.Align.LEFT
                    drawLineText(canvas, line.text, x, y, textPaint, strokePaint, shadowPaint)
                }
                TextJustification.CENTER -> {
                    val x = 0f
                    textPaint.textAlign = Paint.Align.CENTER
                    strokePaint?.textAlign = Paint.Align.CENTER
                    shadowPaint?.textAlign = Paint.Align.CENTER
                    drawLineText(canvas, line.text, x, y, textPaint, strokePaint, shadowPaint)
                }
                TextJustification.RIGHT -> {
                    val x = boxW / 2f
                    textPaint.textAlign = Paint.Align.RIGHT
                    strokePaint?.textAlign = Paint.Align.RIGHT
                    shadowPaint?.textAlign = Paint.Align.RIGHT
                    drawLineText(canvas, line.text, x, y, textPaint, strokePaint, shadowPaint)
                }
                TextJustification.JUSTIFY -> {
                    // For the last line of a paragraph, left-align per standard typography
                    if (line.isParagraphEnd || !line.text.contains(" ")) {
                        val x = -boxW / 2f
                        textPaint.textAlign = Paint.Align.LEFT
                        strokePaint?.textAlign = Paint.Align.LEFT
                        shadowPaint?.textAlign = Paint.Align.LEFT
                        drawLineText(canvas, line.text, x, y, textPaint, strokePaint, shadowPaint)
                    } else {
                        // Distribute extra spacing evenly between words across boxW
                        drawJustifiedLine(canvas, line.text, -boxW / 2f, boxW, y, textPaint, strokePaint, shadowPaint)
                    }
                }
            }
        }
    }

    private fun drawLineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        textPaint: Paint,
        strokePaint: Paint?,
        shadowPaint: Paint?
    ) {
        if (text.isEmpty()) return
        if (shadowPaint != null) {
            canvas.drawText(text, x, y, shadowPaint)
        }
        canvas.drawText(text, x, y, textPaint)
        if (strokePaint != null) {
            canvas.drawText(text, x, y, strokePaint)
        }
    }

    private fun drawJustifiedLine(
        canvas: Canvas,
        text: String,
        leftX: Float,
        targetWidth: Float,
        y: Float,
        textPaint: Paint,
        strokePaint: Paint?,
        shadowPaint: Paint?
    ) {
        val words = text.split(" ")
        if (words.size <= 1) {
            textPaint.textAlign = Paint.Align.LEFT
            strokePaint?.textAlign = Paint.Align.LEFT
            shadowPaint?.textAlign = Paint.Align.LEFT
            drawLineText(canvas, text, leftX, y, textPaint, strokePaint, shadowPaint)
            return
        }

        val totalWordsWidth = words.sumOf { textPaint.measureText(it).toDouble() }.toFloat()
        val extraSpace = max(0f, targetWidth - totalWordsWidth)
        val spaceWidth = extraSpace / (words.size - 1)

        textPaint.textAlign = Paint.Align.LEFT
        strokePaint?.textAlign = Paint.Align.LEFT
        shadowPaint?.textAlign = Paint.Align.LEFT

        var curX = leftX
        for (i in words.indices) {
            val word = words[i]
            drawLineText(canvas, word, curX, y, textPaint, strokePaint, shadowPaint)
            curX += textPaint.measureText(word) + spaceWidth
        }
    }
}
