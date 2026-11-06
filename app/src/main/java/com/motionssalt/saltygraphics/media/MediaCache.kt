package com.motionssalt.saltygraphics.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File

object MediaCache {
    private val memoryCache = LruCache<String, Bitmap>(30)

    fun get(key: String): Bitmap? = memoryCache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
    }

    fun getOrLoad(context: Context, projectId: String, relativeAssetPath: String): Bitmap? {
        val key = "$projectId:$relativeAssetPath"
        memoryCache.get(key)?.let { return it }

        val cleanPath = relativeAssetPath.removePrefix("assets/")
        val file = File(File(context.filesDir, "projects"), "${projectId}_assets/$cleanPath")
        if (!file.exists()) return null

        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                memoryCache.put(key, bitmap)
            }
            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    fun clear(projectId: String? = null) {
        if (projectId == null) {
            memoryCache.evictAll()
        } else {
            val prefix = "$projectId:"
            val snapshot = memoryCache.snapshot()
            for (k in snapshot.keys) {
                if (k.startsWith(prefix)) {
                    memoryCache.remove(k)
                }
            }
        }
    }
}
