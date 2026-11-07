package com.motionssalt.saltygraphics.engine.file

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

sealed interface ProjectLoadState {
    data class Loading(val phase: String = "Starting", val step: Int = 0, val total: Int = 4): ProjectLoadState
    data class Loaded(val result: SgImportResult, val safeMode: Boolean): ProjectLoadState
    data class Error(val message: String): ProjectLoadState
}

/**
 * Bounded blocking-IO bridge. A codec ignoring interruption cannot hold the UI in Loading forever.
 * At most two daemon workers can be stuck; exhausted capacity is a recoverable error, not a queue.
 */
class ProjectLoader(private val timeoutMillis: Long = 15_000,
                    private val executor: ExecutorService = workers) {
    class Context internal constructor(private val cancelled: AtomicBoolean,
                                       private val report: (ProjectLoadState.Loading) -> Unit) {
        fun checkActive() { if(cancelled.get() || Thread.currentThread().isInterrupted) throw CancellationException("Project load cancelled") }
        fun phase(label: String, step: Int) { checkActive();report(ProjectLoadState.Loading(label,step)) }
    }

    suspend fun load(safeMode: Boolean, onProgress: (ProjectLoadState.Loading) -> Unit,
                     work: (Context) -> SgImportResult): ProjectLoadState {
        onProgress(ProjectLoadState.Loading())
        return withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                val cancelled=AtomicBoolean(false)
                val context=Context(cancelled) { if(continuation.isActive) onProgress(it) }
                try {
                    val task=executor.submit {
                        val state=try {
                            val result=work(context);context.checkActive()
                            ProjectLoadState.Loaded(result,safeMode)
                        } catch(e: Exception) {
                            ProjectLoadState.Error(e.message?.take(300) ?: "Could not open project")
                        }
                        if(continuation.isActive) continuation.resume(state)
                    }
                    continuation.invokeOnCancellation {cancelled.set(true);task.cancel(true)}
                } catch(e: RejectedExecutionException) {
                    continuation.resume(ProjectLoadState.Error("Project reader is busy. Please retry or go back."))
                }
            }
        } ?: ProjectLoadState.Error("Opening timed out after ${timeoutMillis/1000} seconds. Retry or open in safe mode.")
    }

    companion object {
        private val workers=ThreadPoolExecutor(0,2,30,TimeUnit.SECONDS,SynchronousQueue(),
            ThreadFactory { task -> Thread(task,"salty-project-reader").apply {isDaemon=true} },ThreadPoolExecutor.AbortPolicy())
    }
}
