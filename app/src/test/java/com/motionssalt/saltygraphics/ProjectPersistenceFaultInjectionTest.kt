package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.data.ProjectRepository
import com.motionssalt.saltygraphics.engine.action.ActionTree
import com.motionssalt.saltygraphics.engine.file.ProjectLoadState
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.CanvasSize
import com.motionssalt.saltygraphics.engine.model.Layer
import com.motionssalt.saltygraphics.engine.model.LayerType
import com.motionssalt.saltygraphics.engine.model.ShapeData
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProjectPersistenceFaultInjectionTest {

    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun testCorruptOrDeletedIndexRebuiltFromDiskFiles() = runBlocking {
        val repo = ProjectRepository(context)
        val doc1 = repo.createProject("Project Alpha", CanvasSize(1080, 1080))
        val doc2 = repo.createProject("Project Beta", CanvasSize(1920, 1080))
        val doc3 = repo.createProject("Project Gamma", CanvasSize(800, 600))

        assertEquals(3, repo.projects.value.size)

        // Fault injection 1: Completely delete index.json
        val indexFile = File(context.filesDir, "projects/index.json")
        assertTrue(indexFile.exists())
        indexFile.delete()
        assertFalse(indexFile.exists())

        // Re-load projects from disk
        repo.loadProjects()
        assertEquals("Index must be rebuilt with all 3 projects after index deletion", 3, repo.projects.value.size)
        assertTrue(repo.projects.value.any { it.id == doc1.id })
        assertTrue(repo.projects.value.any { it.id == doc2.id })
        assertTrue(repo.projects.value.any { it.id == doc3.id })
        assertTrue("Index file must be restored", indexFile.exists())

        // Fault injection 2: Corrupt index.json with garbage bytes
        indexFile.writeBytes(byteArrayOf(0, 1, 2, 3, 4, 5, 99, 100))
        repo.loadProjects()
        assertEquals("Index must be rebuilt with all 3 projects after index corruption", 3, repo.projects.value.size)
    }

    @Test
    fun testTruncatedMainFileAutomaticallyRecoversFromBackup() = runBlocking {
        val repo = ProjectRepository(context)
        val doc = repo.createProject("Resilient Design", CanvasSize(512, 512))
        val layer = Layer(id = "layer-1", name = "Test Shape", type = LayerType.SHAPE, shapeData = ShapeData())
        val updatedDoc = doc.addLayer(layer)
        repo.saveProject(updatedDoc)

        val mainFile = repo.getProjectFile(doc.id)
        val backupFile = repo.getBackupFile(doc.id)
        assertTrue("Main project file must exist", mainFile.exists() && mainFile.length() > 0)
        assertTrue("Backup project file must exist", backupFile.exists() && backupFile.length() > 0)

        // Fault injection: Truncate main file to 0 bytes (simulating interrupted write or crash)
        mainFile.writeBytes(ByteArray(0))
        assertEquals(0L, mainFile.length())

        // Re-open project: must seamlessly recover from backup
        val loadState = repo.openProject(doc.id)
        assertTrue("Should open successfully using backup copy", loadState is ProjectLoadState.Loaded)
        val loadedDoc = (loadState as ProjectLoadState.Loaded).result.document
        assertEquals("Resilient Design", loadedDoc.name)
        assertEquals(1, loadedDoc.layers.size)
        assertEquals("layer-1", loadedDoc.layers.first().id)
        assertTrue("Main file should be restored from backup", mainFile.length() > 0L)
    }

    @Test
    fun testScopeCancellationDuringSaveDoesNotCorruptFileOrIndex() = runBlocking {
        val repo = ProjectRepository(context)
        val doc = repo.createProject("Safe Scope Project", CanvasSize(800, 800))

        // Launch in an ephemeral scope and cancel it immediately while save is triggered
        val cancellableScope = CoroutineScope(Dispatchers.IO)
        val saveJob = cancellableScope.launch {
            val updated = doc.copy(name = "Updated Safely")
            repo.saveProject(updated)
        }
        // Cancel the scope immediately
        cancellableScope.cancel()

        // Wait a short moment for NonCancellable IO block in repository to finish
        delay(150)

        // Verify project file and backup file are both intact and valid archives
        val mainFile = repo.getProjectFile(doc.id)
        assertTrue(mainFile.exists() && mainFile.length() > 0)
        val imported = SgProjectArchive.loadFromFile(mainFile)
        assertEquals("Updated Safely", imported.document.name)

        // Verify project is in the index
        repo.loadProjects()
        assertTrue(repo.projects.value.any { it.id == doc.id && it.name == "Updated Safely" })
    }

    @Test
    fun testSaveProjectInBackgroundSurvivesScreenExit() = runBlocking {
        val repo = ProjectRepository(context)
        val doc = repo.createProject("Background Save Project", CanvasSize(1000, 1000))
        val tree = ActionTree(doc)
        tree.recordAction("Add Layer", doc.addLayer(Layer(name = "Layer B")))

        val job = repo.saveProjectInBackground(tree.currentDocument, tree.toSnapshot())
        job.join()

        val mainFile = repo.getProjectFile(doc.id)
        val imported = SgProjectArchive.loadFromFile(mainFile)
        assertEquals(1, imported.document.layers.size)
        assertNotNull(imported.historySnapshot)
    }

    @Test
    fun testNoPathDeletesProjectsOutsideExplicitDelete() = runBlocking {
        val repo = ProjectRepository(context)
        // Project matching the old seed name and layer count
        val seedLike = repo.createProject("My First Salty Design", CanvasSize(1080, 1920))
        val withTwoLayers = seedLike.addLayer(Layer(name = "L1")).addLayer(Layer(name = "L2"))
        repo.saveProject(withTwoLayers)

        val userProject = repo.createProject("Victor Artwork", CanvasSize(2000, 2000))

        // Multiple startup/load cycles
        repo.loadProjects()
        repo.loadProjects()
        repo.loadProjects()

        // Verify both projects are still present
        assertTrue("Seed-named project must NOT be deleted", repo.projects.value.any { it.id == seedLike.id })
        assertTrue("User project must NOT be deleted", repo.projects.value.any { it.id == userProject.id })

        // Only explicit deletion removes projects
        repo.deleteProjects(setOf(seedLike.id))
        repo.loadProjects()
        assertFalse("Explicitly deleted project must be gone", repo.projects.value.any { it.id == seedLike.id })
        assertTrue("Untargeted project remains intact", repo.projects.value.any { it.id == userProject.id })
    }
}
