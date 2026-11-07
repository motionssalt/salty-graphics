package com.motionssalt.saltygraphics

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import com.motionssalt.saltygraphics.data.*
import com.motionssalt.saltygraphics.engine.file.*
import com.motionssalt.saltygraphics.engine.model.*
import com.motionssalt.saltygraphics.ui.editor.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34],qualifiers="w412dp-h860dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ProjectLoadingRegressionTest {
    @get:Rule val compose=createEmptyComposeRule()
    private val context get()=RuntimeEnvironment.getApplication()

    @Test fun missingCorruptAndHalfWrittenProjectsFinishWithRecoverableErrorsAndSafeModeKeepsHealthyLayers() = runBlocking<Unit> {
        val repo=ProjectRepository(context,ProjectLoader(2000))
        val doc=repo.createProject("Load fixture",CanvasSize(256,256))
        val healthy=Layer(id="healthy",name="Healthy",shapeData=ShapeData())
        val bad=Layer(id="bad",name="Missing image",type=LayerType.MEDIA,mediaAssetPath="missing.png")
        SgProjectArchive.saveToFile(doc.copy(layers=listOf(healthy,bad)),repo.getProjectFile(doc.id))
        assertTrue(repo.openProject(doc.id) is ProjectLoadState.Error)
        val safe=repo.openProject(doc.id,true) as ProjectLoadState.Loaded
        assertEquals(healthy,safe.result.document.layers.first())
        assertEquals(LayerType.QUARANTINED,safe.result.document.layers.last().type)
        assertFalse(safe.result.document.layers.last().isVisible)
        assertNotNull(safe.result.document.layers.last().quarantinedData)
        // Safe-mode opening never overwrites the original archive.
        assertEquals(LayerType.MEDIA,SgProjectArchive.loadFromFile(repo.getProjectFile(doc.id)).document.layers.last().type)
        repo.getProjectFile(doc.id).writeBytes(byteArrayOf(80,75,3,4,1))
        assertTrue(repo.openProject(doc.id) is ProjectLoadState.Error)
        assertTrue(repo.openProject("does-not-exist") is ProjectLoadState.Error)
        assertFalse(repo.isProjectLoading(doc.id))
    }

    @Test fun corruptSerializedLayerIsQuarantinedWithoutLosingHealthySibling() = runBlocking<Unit> {
        val repo=ProjectRepository(context,ProjectLoader(2000))
        val doc=repo.createProject("Bad layer",CanvasSize(256,256))
        ZipOutputStream(repo.getProjectFile(doc.id).outputStream()).use {zip->
            zip.putNextEntry(ZipEntry("project.json"))
            zip.write("""{"id":"${doc.id}","layers":[{"id":"good","name":"Good","type":"SHAPE"},{"id":"bad","type":"UNKNOWN_BROKEN_TYPE"}]}""".toByteArray())
            zip.closeEntry()
        }
        assertTrue(repo.openProject(doc.id) is ProjectLoadState.Error)
        val loaded=repo.openProject(doc.id,true) as ProjectLoadState.Loaded
        assertEquals("good",loaded.result.document.layers.first().id)
        assertEquals(LayerType.QUARANTINED,loaded.result.document.layers.last().type)
    }

    @Test fun autosaveWaitsForLoadAndBlockedThumbnailNeverBlocksForegroundReader() = runBlocking<Unit> {
        val decodeStarted=CountDownLatch(1);val allowDecode=CountDownLatch(1)
        val thumbStarted=CountDownLatch(1);val allowThumbnail=CountDownLatch(1)
        val repo=ProjectRepository(context,ProjectLoader(1500),
            decodeMedia={_,_,_->decodeStarted.countDown();allowDecode.await(3,TimeUnit.SECONDS);true},
            renderThumbnail={_,_->thumbStarted.countDown();allowThumbnail.await(3,TimeUnit.SECONDS);byteArrayOf(1,2,3)})
        val doc=repo.createProject("Concurrent",CanvasSize(256,256))
        val media=Layer(type=LayerType.MEDIA,mediaAssetPath="test.png")
        SgProjectArchive.saveToFile(doc.copy(layers=listOf(media)),repo.getProjectFile(doc.id),mapOf("test.png" to byteArrayOf(1)))
        try {
            val opening=async(Dispatchers.Default) {repo.openProject(doc.id)}
            assertTrue(decodeStarted.await(2,TimeUnit.SECONDS))
            assertTrue(repo.isProjectLoading(doc.id))
            val saving=async(Dispatchers.IO) {repo.saveProject(doc.copy(name="Saved while open"))}
            delay(100)
            assertFalse(saving.isCompleted)
            assertEquals("Concurrent",SgProjectArchive.loadFromFile(repo.getProjectFile(doc.id)).document.name)
            allowDecode.countDown()
            assertTrue(opening.await() is ProjectLoadState.Loaded)
            saving.await()
            assertTrue(thumbStarted.await(2,TimeUnit.SECONDS))
            val duration=measureTimeMillis { assertTrue(repo.openProject(doc.id) is ProjectLoadState.Loaded) }
            assertTrue("Open waited for thumbnail $duration ms",duration<1000)
            assertEquals("Saved while open",SgProjectArchive.loadFromFile(repo.getProjectFile(doc.id)).document.name)
        } finally {allowDecode.countDown();allowThumbnail.countDown()}
    }

    @Test fun slowAssetTimeoutDoesNotStickSpinnerOrAcceptLateLoadedResult() = runBlocking<Unit> {
        val release=CountDownLatch(1)
        val repo=ProjectRepository(context,ProjectLoader(200),decodeMedia={_,_,_->
            while(release.count>0) {try {release.await(20,TimeUnit.MILLISECONDS)}catch(_:InterruptedException){}}
            true
        })
        val doc=repo.createProject("Slow",CanvasSize(256,256))
        SgProjectArchive.saveToFile(doc.copy(layers=listOf(Layer(id="healthy",shapeData=ShapeData()),Layer(type=LayerType.MEDIA,mediaAssetPath="slow.png"))),
            repo.getProjectFile(doc.id),mapOf("slow.png" to byteArrayOf(1)))
        try {
            val elapsed=measureTimeMillis {assertTrue(repo.openProject(doc.id) is ProjectLoadState.Error)}
            assertTrue(elapsed<1500);assertFalse(repo.isProjectLoading(doc.id))
            val report=DeviceInfoProvider(context).composeProblemReport("Stuck project").getStringExtra(Intent.EXTRA_TEXT)!!
            assertTrue(report.contains("project_load"));assertTrue(report.contains("elapsed_ms"));assertTrue(report.contains("timed out"))
            val safe=repo.openProject(doc.id,true) as ProjectLoadState.Loaded
            assertEquals("healthy",safe.result.document.layers.first().id)
            assertEquals(LayerType.QUARANTINED,safe.result.document.layers.last().type)
        } finally {release.countDown()}
    }

    @Test fun errorSurfaceOffersRetrySafeModeAndBackAndRealEditorMissingProjectStopsLoading() {
        val host=Robolectric.buildActivity(ComponentActivity::class.java).setup()
        var retry=0;var safe=0;var back=0
        try {
            host.get().setContent {MaterialTheme {ProjectLoadingPanel(ProjectLoadState.Error("Fixture error"),{retry++},{safe++},{back++})}}
            compose.onNodeWithText("Retry").performClick()
            compose.onNodeWithText("Open in safe mode").performClick()
            compose.onNodeWithText("Back").performClick()
            assertEquals(1,retry);assertEquals(1,safe);assertEquals(1,back)
            host.get().setContent {MaterialTheme {EditorScreen("absent",repository=ProjectRepository(context,ProjectLoader(1000)),
                onNavigateBack={back++},onNavigateToExport={},onNavigateToSettings={})}}
            compose.waitUntil(3000) {compose.onAllNodesWithText("Could not open project").fetchSemanticsNodes().isNotEmpty()}
            compose.onNodeWithText("Retry").assertIsDisplayed()
            compose.onNodeWithText("Open in safe mode").assertIsDisplayed()
        } finally {host.pause().stop().destroy()}
    }
}
