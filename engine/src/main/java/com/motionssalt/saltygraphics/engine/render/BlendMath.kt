package com.motionssalt.saltygraphics.engine.render

import com.motionssalt.saltygraphics.engine.model.BlendMode
import kotlin.math.*

object BlendMath {

    fun blendChannel(mode: BlendMode, b: Float, s: Float): Float {
        val bClamped = b.coerceIn(0f, 1f)
        val sClamped = s.coerceIn(0f, 1f)

        return when (mode) {
            BlendMode.NORMAL -> sClamped
            BlendMode.DARKEN -> min(bClamped, sClamped)
            BlendMode.MULTIPLY -> bClamped * sClamped
            BlendMode.COLOR_BURN -> {
                if (bClamped >= 1f) 1f
                else if (sClamped <= 0f) 0f
                else (1f - min(1f, (1f - bClamped) / sClamped)).coerceIn(0f, 1f)
            }
            BlendMode.LINEAR_BURN -> max(0f, bClamped + sClamped - 1f)
            BlendMode.LIGHTEN -> max(bClamped, sClamped)
            BlendMode.SCREEN -> bClamped + sClamped - (bClamped * sClamped)
            BlendMode.COLOR_DODGE -> {
                if (bClamped <= 0f) 0f
                else if (sClamped >= 1f) 1f
                else min(1f, bClamped / (1f - sClamped)).coerceIn(0f, 1f)
            }
            BlendMode.LINEAR_DODGE -> min(1f, bClamped + sClamped)
            BlendMode.OVERLAY -> {
                if (bClamped <= 0.5f) 2f * bClamped * sClamped
                else 1f - 2f * (1f - bClamped) * (1f - sClamped)
            }
            BlendMode.SOFT_LIGHT -> {
                if (sClamped <= 0.5f) {
                    bClamped - (1f - 2f * sClamped) * bClamped * (1f - bClamped)
                } else {
                    val d = if (bClamped <= 0.25f) ((16f * bClamped - 12f) * bClamped + 4f) * bClamped else sqrt(bClamped)
                    bClamped + (2f * sClamped - 1f) * (d - bClamped)
                }
            }
            BlendMode.HARD_LIGHT -> {
                if (sClamped <= 0.5f) 2f * bClamped * sClamped
                else 1f - 2f * (1f - bClamped) * (1f - sClamped)
            }
            BlendMode.VIVID_LIGHT -> {
                if (sClamped <= 0.5f) {
                    val s2 = 2f * sClamped
                    if (bClamped >= 1f) 1f
                    else if (s2 <= 0f) 0f
                    else (1f - min(1f, (1f - bClamped) / s2)).coerceIn(0f, 1f)
                } else {
                    val s2 = 2f * (sClamped - 0.5f)
                    if (bClamped <= 0f) 0f
                    else if (s2 >= 1f) 1f
                    else min(1f, bClamped / (1f - s2)).coerceIn(0f, 1f)
                }
            }
            BlendMode.LINEAR_LIGHT -> (bClamped + 2f * sClamped - 1f).coerceIn(0f, 1f)
            BlendMode.PIN_LIGHT -> {
                if (sClamped <= 0.5f) min(bClamped, 2f * sClamped)
                else max(bClamped, 2f * (sClamped - 0.5f))
            }
            BlendMode.DIFFERENCE -> abs(bClamped - sClamped)
            BlendMode.EXCLUSION -> bClamped + sClamped - 2f * bClamped * sClamped
            BlendMode.SUBTRACT -> max(0f, bClamped - sClamped)
            BlendMode.DIVIDE -> {
                if (bClamped <= 0f) 0f
                else if (sClamped <= 0f) 1f
                else min(1f, bClamped / sClamped)
            }
            // Component blend modes handled in blendColor
            BlendMode.HUE, BlendMode.SATURATION, BlendMode.COLOR, BlendMode.LUMINOSITY, BlendMode.ALPHA_CUTOUT_INWARD, BlendMode.ALPHA_CUTOUT_OUTWARD -> sClamped
        }.coerceIn(0f, 1f)
    }

    /**
     * Blends backdrop ARGB with source ARGB including opacity and alpha compositing.
     */
    fun blendColor(mode: BlendMode, backdropArgb: Long, sourceArgb: Long, opacity: Float = 1f): Long {
        val bA = ((backdropArgb shr 24 and 0xFF) / 255f).coerceIn(0f, 1f)
        val bR = ((backdropArgb shr 16 and 0xFF) / 255f)
        val bG = ((backdropArgb shr 8 and 0xFF) / 255f)
        val bB = ((backdropArgb and 0xFF) / 255f)

        val sRawA = ((sourceArgb shr 24 and 0xFF) / 255f).coerceIn(0f, 1f)
        val sA = (sRawA * opacity.coerceIn(0f, 1f)).coerceIn(0f, 1f)
        val sR = ((sourceArgb shr 16 and 0xFF) / 255f)
        val sG = ((sourceArgb shr 8 and 0xFF) / 255f)
        val sB = ((sourceArgb and 0xFF) / 255f)

        if (mode == BlendMode.ALPHA_CUTOUT_INWARD) {
            val newA = (bA * (1f - sA)).coerceIn(0f, 1f)
            val outA = (newA * 255f).roundToInt()
            return ((outA.toLong() and 0xFF) shl 24) or (backdropArgb and 0x00FFFFFF)
        }
        if (mode == BlendMode.ALPHA_CUTOUT_OUTWARD) {
            val newA = (bA * sA).coerceIn(0f, 1f)
            val outA = (newA * 255f).roundToInt()
            return ((outA.toLong() and 0xFF) shl 24) or (backdropArgb and 0x00FFFFFF)
        }
        if (sA <= 0f) return backdropArgb
        if (bA <= 0f) {
            val outA = (sA * 255f).roundToInt()
            val outR = (sR * 255f).roundToInt()
            val outG = (sG * 255f).roundToInt()
            val outB = (sB * 255f).roundToInt()
            return ((outA.toLong() and 0xFF) shl 24) or
                   ((outR.toLong() and 0xFF) shl 16) or
                   ((outG.toLong() and 0xFF) shl 8) or
                   (outB.toLong() and 0xFF)
        }

        val (blendedR, blendedG, blendedB) = when (mode) {
            BlendMode.HUE -> blendHsl(bR, bG, bB, sR, sG, sB, 0)
            BlendMode.SATURATION -> blendHsl(bR, bG, bB, sR, sG, sB, 1)
            BlendMode.COLOR -> blendHsl(bR, bG, bB, sR, sG, sB, 2)
            BlendMode.LUMINOSITY -> blendHsl(bR, bG, bB, sR, sG, sB, 3)
            else -> Triple(
                blendChannel(mode, bR, sR),
                blendChannel(mode, bG, sG),
                blendChannel(mode, bB, sB)
            )
        }

        // Standard W3C / Porter-Duff source-over compositing with blend equation:
        val outAlpha = sA + bA * (1f - sA)
        val outR = if (outAlpha > 0f) ((1f - sA) * bA * bR + sA * (1f - bA) * sR + sA * bA * blendedR) / outAlpha else 0f
        val outG = if (outAlpha > 0f) ((1f - sA) * bA * bG + sA * (1f - bA) * sG + sA * bA * blendedG) / outAlpha else 0f
        val outB = if (outAlpha > 0f) ((1f - sA) * bA * bB + sA * (1f - bA) * sB + sA * bA * blendedB) / outAlpha else 0f

        val aInt = (outAlpha * 255f).roundToInt().coerceIn(0, 255)
        val rInt = (outR * 255f).roundToInt().coerceIn(0, 255)
        val gInt = (outG * 255f).roundToInt().coerceIn(0, 255)
        val bInt = (outB * 255f).roundToInt().coerceIn(0, 255)

        return ((aInt.toLong() and 0xFF) shl 24) or
               ((rInt.toLong() and 0xFF) shl 16) or
               ((gInt.toLong() and 0xFF) shl 8) or
               (bInt.toLong() and 0xFF)
    }

    private fun lum(r: Float, g: Float, b: Float): Float = 0.299f * r + 0.587f * g + 0.114f * b

    private fun setLum(r: Float, g: Float, b: Float, l: Float): Triple<Float, Float, Float> {
        val d = l - lum(r, g, b)
        var nr = r + d
        var ng = g + d
        var nb = b + d
        val nl = lum(nr, ng, nb)
        val minC = min(nr, min(ng, nb))
        val maxC = max(nr, max(ng, nb))
        if (minC < 0f) {
            nr = nl + ((nr - nl) * nl) / (nl - minC)
            ng = nl + ((ng - nl) * nl) / (nl - minC)
            nb = nl + ((nb - nl) * nl) / (nl - minC)
        }
        if (maxC > 1f) {
            nr = nl + ((nr - nl) * (1f - nl)) / (maxC - nl)
            ng = nl + ((ng - nl) * (1f - nl)) / (maxC - nl)
            nb = nl + ((nb - nl) * (1f - nl)) / (maxC - nl)
        }
        return Triple(nr.coerceIn(0f, 1f), ng.coerceIn(0f, 1f), nb.coerceIn(0f, 1f))
    }

    private fun sat(r: Float, g: Float, b: Float): Float = max(r, max(g, b)) - min(r, min(g, b))

    private fun blendHsl(br: Float, bg: Float, bb: Float, sr: Float, sg: Float, sb: Float, mode: Int): Triple<Float, Float, Float> {
        return when (mode) {
            0 -> setLum(sr, sg, sb, lum(br, bg, bb)) // Hue
            1 -> { // Saturation
                val s = sat(sr, sg, sb)
                val l = lum(br, bg, bb)
                setLum(br * s, bg * s, bb * s, l)
            }
            2 -> setLum(sr, sg, sb, lum(br, bg, bb)) // Color
            3 -> setLum(br, bg, bb, lum(sr, sg, sb)) // Luminosity
            else -> Triple(sr, sg, sb)
        }
    }
}
