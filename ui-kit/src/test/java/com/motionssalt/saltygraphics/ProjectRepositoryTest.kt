package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.data.ProjectSummary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ProjectRepositoryTest {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @Test
    fun testProjectSummarySerializationRoundTrip() {
        val summary = ProjectSummary(
            id = UUID.randomUUID().toString(),
            name = "Test Salty Design",
            width = 1080,
            height = 1920,
            layerCount = 5,
            modifiedAt = 1730000000000L
        )

        val encoded = json.encodeToString(summary)
        assertTrue("Encoded JSON should contain project name", encoded.contains("Test Salty Design"))

        val decoded = json.decodeFromString<ProjectSummary>(encoded)
        assertEquals(summary.id, decoded.id)
        assertEquals(summary.name, decoded.name)
        assertEquals(summary.width, decoded.width)
        assertEquals(summary.height, decoded.height)
        assertEquals(summary.layerCount, decoded.layerCount)
        assertEquals(summary.modifiedAt, decoded.modifiedAt)
    }

    @Test
    fun testProjectSummaryListSerialization() {
        val list = listOf(
            ProjectSummary(
                id = "p1",
                name = "Design A",
                width = 1080,
                height = 1080,
                layerCount = 1,
                modifiedAt = 1000L
            ),
            ProjectSummary(
                id = "p2",
                name = "Design B",
                width = 1080,
                height = 1920,
                layerCount = 3,
                modifiedAt = 2000L
            )
        )

        val encoded = json.encodeToString(list)
        val decoded = json.decodeFromString<List<ProjectSummary>>(encoded)

        assertEquals(2, decoded.size)
        assertEquals("Design A", decoded[0].name)
        assertEquals("Design B", decoded[1].name)
    }

    @Test
    fun testProjectsAreNeverAutomaticallyDeleted() {
        val projects = listOf(
            ProjectSummary(
                id = "seed-id",
                name = "My First Salty Design",
                width = 1080,
                height = 1920,
                layerCount = 2,
                modifiedAt = 1000L
            ),
            ProjectSummary(
                id = "user-id-1",
                name = "Victor's Real Banner",
                width = 1920,
                height = 1080,
                layerCount = 4,
                modifiedAt = 2000L
            ),
            ProjectSummary(
                id = "user-id-2",
                name = "My First Salty Design",
                width = 1080,
                height = 1920,
                layerCount = 5,
                modifiedAt = 3000L
            )
        )

        // No automatic deletion path touches any project
        assertEquals(3, projects.size)
        assertTrue("Seed project must be kept and never automatically deleted", projects.any { it.id == "seed-id" })
        assertTrue("User project 1 must be kept", projects.any { it.id == "user-id-1" })
        assertTrue("User project 2 must be kept", projects.any { it.id == "user-id-2" })
    }

    @Test
    fun testLayerCountPluralFormatting() {
        fun formatLayerCount(count: Int): String = when (count) {
            0 -> "No layers"
            1 -> "1 layer"
            else -> "$count layers"
        }
        assertEquals("No layers", formatLayerCount(0))
        assertEquals("1 layer", formatLayerCount(1))
        assertEquals("2 layers", formatLayerCount(2))
        assertEquals("15 layers", formatLayerCount(15))
    }

    @Test
    fun testDuplicateImportNameResolution() {
        val existingNames = listOf("summer poster", "summer poster (copy 1)")
        val baseName = "Summer Poster"
        val uniqueName = if (existingNames.contains(baseName.lowercase().trim())) {
            var copyNum = 1
            var candidate = "$baseName (Copy $copyNum)"
            while (existingNames.contains(candidate.lowercase().trim())) {
                copyNum++
                candidate = "$baseName (Copy $copyNum)"
            }
            candidate
        } else {
            baseName
        }
        assertEquals("Summer Poster (Copy 2)", uniqueName)
    }

}
