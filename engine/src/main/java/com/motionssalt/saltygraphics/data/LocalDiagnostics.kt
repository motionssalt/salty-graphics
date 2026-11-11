package com.motionssalt.saltygraphics.data

import android.content.Context
import android.os.SystemClock
import java.io.File
import java.time.Instant

/** Shared with round-1 crash report attachment. No secrets or document content are logged. */
object LocalDiagnostics {
    @Synchronized fun record(context: Context, message: String) {
        runCatching {
            val file=File(context.filesDir,"last_crash.txt")
            if(file.exists() && file.length()>64*1024) file.writeText(file.readText().takeLast(32*1024))
            file.appendText("${Instant.now()} ${message.replace('\n',' ').take(500)}\n")
        }
    }
    fun clockMillis(): Long = SystemClock.elapsedRealtime()
}
