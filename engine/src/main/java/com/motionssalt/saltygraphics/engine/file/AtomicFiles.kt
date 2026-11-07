package com.motionssalt.saltygraphics.engine.file

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Write on the same filesystem, fsync and atomically rename. A failed write preserves the old file. */
object AtomicFiles {
    fun write(file: File, writer: (OutputStream) -> Unit) {
        val parent=file.absoluteFile.parentFile ?: error("Missing parent directory")
        parent.mkdirs()
        val temporary=File.createTempFile(".${file.name}.",".tmp",parent)
        try {
            FileOutputStream(temporary).use { stream -> writer(stream);stream.flush();stream.fd.sync() }
            Files.move(temporary.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)
        } finally { temporary.delete() }
    }
}
