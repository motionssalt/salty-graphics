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
}
