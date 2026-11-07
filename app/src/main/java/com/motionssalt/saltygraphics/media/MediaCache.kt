package com.motionssalt.saltygraphics.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object MediaCache {
    private val memoryCache = LruCache<String, Bitmap>(30)
    private val pending=ConcurrentHashMap.newKeySet<String>()
    private val failed=ConcurrentHashMap.newKeySet<String>()
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val _revision=MutableStateFlow(0L)
    val revision=_revision.asStateFlow()

    /** Canvas never invokes a blocking decoder, even after a texture-cache eviction. */
    fun getOrRequest(context: Context, projectId: String, relativeAssetPath: String): Bitmap? {
        val key="$projectId:$relativeAssetPath"
        memoryCache.get(key)?.let {return it}
        if(!failed.contains(key) && pending.add(key)) scope.launch {
            try {if(getOrLoad(context.applicationContext,projectId,relativeAssetPath)==null) failed.add(key)}
            finally {pending.remove(key)}
        }
        return null
    }

    fun get(key: String): Bitmap? = memoryCache.get(key)

    fun put(key: String, bitmap: Bitmap) {
        memoryCache.put(key, bitmap)
        synchronized(_revision) {_revision.value++}
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
                put(key, bitmap)
            }
            bitmap
        } catch (_: Throwable) {
            null
        }
    }

    fun clear(projectId: String? = null) {
        if(projectId==null) failed.clear() else failed.removeIf {it.startsWith("$projectId:")}
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
