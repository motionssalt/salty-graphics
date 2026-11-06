package com.motionssalt.saltygraphics.engine.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.opengl.GLES30
import com.motionssalt.saltygraphics.engine.model.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * OpenGL ES 3.0 Rendering Engine with FBO Proxy and Texture Caching.
 */
class GLRenderEngine {
    private var maxTextureSize: Int = 4096
    private var isInitialized = false

    // Texture cache: layerId -> CachedTexture
    private val textureCache = mutableMapOf<String, CachedTexture>()

    // FBO handles for offscreen compositing passes
    private var fboId = 0
    private var fboTextureId = 0
    private var currentFboWidth = 0
    private var currentFboHeight = 0

    data class CachedTexture(
        val textureId: Int,
        val width: Int,
        val height: Int,
        val versionToken: Int
    )

    fun initialize() {
        val max = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_MAX_TEXTURE_SIZE, max, 0)
        maxTextureSize = if (max[0] > 0) max[0] else 4096
        isInitialized = true
    }

    fun getMaxTextureSize(): Int = maxTextureSize

    /**
     * Determines safe dimension given GPU limits to guarantee the app never crashes on 8K canvases.
     */
    fun clampToMaxTextureSize(requestedWidth: Int, requestedHeight: Int): Pair<Int, Int> {
        val maxLimit = maxOf(2048, maxTextureSize)
        val largest = maxOf(requestedWidth, requestedHeight)
        if (largest <= maxLimit) return Pair(requestedWidth, requestedHeight)

        val scale = maxLimit.toFloat() / largest
        return Pair((requestedWidth * scale).toInt(), (requestedHeight * scale).toInt())
    }

    /**
     * Prepares offscreen Framebuffer for the given proxy scale.
     */
    fun setupFbo(width: Int, height: Int) {
        if (fboId != 0 && currentFboWidth == width && currentFboHeight == height) {
            return
        }
        deleteFbo()

        val fbos = IntArray(1)
        GLES30.glGenFramebuffers(1, fbos, 0)
        fboId = fbos[0]

        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        fboTextureId = textures[0]

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, fboTextureId)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA,
            width, height, 0,
            GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null
        )
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)

        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, fboId)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER, GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D, fboTextureId, 0
        )
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)

        currentFboWidth = width
        currentFboHeight = height
    }

    fun deleteFbo() {
        if (fboId != 0) {
            GLES30.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
            fboId = 0
        }
        if (fboTextureId != 0) {
            GLES30.glDeleteTextures(1, intArrayOf(fboTextureId), 0)
            fboTextureId = 0
        }
    }

    /**
     * Clears cached texture for a specific layer.
     */
    fun invalidateLayer(layerId: String) {
        val cached = textureCache.remove(layerId)
        if (cached != null) {
            GLES30.glDeleteTextures(1, intArrayOf(cached.textureId), 0)
        }
    }

    fun clearAllCaches() {
        for (cached in textureCache.values) {
            GLES30.glDeleteTextures(1, intArrayOf(cached.textureId), 0)
        }
        textureCache.clear()
        deleteFbo()
    }

    /**
     * Renders a shape layer to an Android software Bitmap buffer (for upload to GL texture or preview).
     */
    fun rasterizeShape(shape: ShapeData): Bitmap {
        val w = maxOf(1, shape.width.toInt())
        val h = maxOf(1, shape.height.toInt())
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shape.fillColor.toInt()
            style = Paint.Style.FILL
        }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = shape.strokeColor.toInt()
            strokeWidth = shape.strokeWidth
            style = Paint.Style.STROKE
        }

        val halfW = w / 2f
        val halfH = h / 2f
        canvas.translate(halfW, halfH)

        val vectorPath = shape.toVectorPath()
        val path = Path()
        val points = vectorPath.points
        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            val segments = if (vectorPath.isClosed) points.size else points.size - 1
            for (i in 0 until segments) {
                val p0 = points[i]
                val p1 = points[(i + 1) % points.size]
                if (p0.hasOutHandle || p1.hasInHandle) {
                    path.cubicTo(p0.outHandleX, p0.outHandleY, p1.inHandleX, p1.inHandleY, p1.x, p1.y)
                } else {
                    path.lineTo(p1.x, p1.y)
                }
            }
            if (vectorPath.isClosed) path.close()
        }

        if (shape.hasFill) canvas.drawPath(path, fillPaint)
        if (shape.hasStroke) canvas.drawPath(path, strokePaint)

        return bitmap
    }
}
