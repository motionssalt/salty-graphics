package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.file.*
import com.motionssalt.saltygraphics.engine.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

class ProjectLoadingTest {
    @Test fun failedAtomicWriteKeepsOriginalAndCleansTemporaryFiles() {
        val dir=Files.createTempDirectory("salty-atomic").toFile()
        try {
            val file=File(dir,"project.sg")
            val original=ProjectDocument(name="Original",layers=listOf(Layer()))
            SgProjectArchive.saveToFile(original,file)
            val bytes=file.readBytes()
            assertTrue(runCatching {AtomicFiles.write(file){it.write(byteArrayOf(1,2,3));error("Interrupted write")}}.isFailure)
            assertArrayEquals(bytes,file.readBytes())
            assertEquals(original,SgProjectArchive.loadFromFile(file).document)
            assertEquals(listOf("project.sg"),dir.list()!!.toList())
        } finally {dir.deleteRecursively()}
    }
    @Test fun concurrentAtomicWritersNeverExposeHalfAnArchive() = runBlocking<Unit> {
        val dir=Files.createTempDirectory("salty-concurrent").toFile();val file=File(dir,"project.sg")
        try {
            SgProjectArchive.saveToFile(ProjectDocument(name="Initial"),file)
            val writers=(0 until 3).map { worker -> async(Dispatchers.IO) {
                repeat(25) {n->SgProjectArchive.saveToFile(ProjectDocument(name="$worker-$n",layers=listOf(Layer())),file)}
            }}
            repeat(150) {val doc=SgProjectArchive.loadFromFile(file).document;assertTrue(doc.name.isNotEmpty())}
            writers.awaitAll()
        } finally {dir.deleteRecursively()}
    }
    @Test fun slowUninterruptibleReaderStillEndsWithTimeoutAndIgnoresLateProgress() = runBlocking<Unit> {
        val release=CountDownLatch(1);val phases=java.util.concurrent.CopyOnWriteArrayList<String>()
        try {
            val elapsed=measureTimeMillis {
                val state=ProjectLoader(150).load(false,{phases.add(it.phase)}) {ctx->
                    ctx.phase("Slow asset",3)
                    while(release.count>0) {try {release.await(25,TimeUnit.MILLISECONDS)} catch(_:InterruptedException) {}}
                    ctx.phase("Late callback",4)
                    SgImportResult(ProjectDocument())
                }
                assertTrue(state is ProjectLoadState.Error)
                assertTrue((state as ProjectLoadState.Error).message.contains("timed out"))
            }
            assertTrue("Timeout took $elapsed ms",elapsed<1500)
            release.countDown();delay(100)
            assertFalse(phases.contains("Late callback"))
        } finally {release.countDown()}
    }
    @Test fun loadingHasOrderedPhasesAndAlwaysTerminalSuccessOrError() = runBlocking<Unit> {
        val phases=mutableListOf<Int>()
        val loader=ProjectLoader(1000)
        val success=loader.load(false,{phases.add(it.step)}) {ctx->
            for(i in 1..4) ctx.phase("Phase $i",i)
            SgImportResult(ProjectDocument(name="Healthy"))
        }
        assertTrue(success is ProjectLoadState.Loaded);assertEquals(listOf(0,1,2,3,4),phases)
        val failed=loader.load(false,{}) {throw IllegalArgumentException("Corrupt archive")}
        assertEquals("Corrupt archive",(failed as ProjectLoadState.Error).message)
    }
}
