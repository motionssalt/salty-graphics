package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.action.ActionNode
import com.motionssalt.saltygraphics.engine.action.ActionTreeSnapshot
import com.motionssalt.saltygraphics.engine.file.SgManifest
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SgRoundTripTest {

    @Test
    fun testCompleteSgArchiveRoundTrip() {
        // 1. Text, Shape, Vector, Media, Custom Canvas Color
        val vectorPoints = listOf(
            BezierPoint(x = -50f, y = -50f, outHandleX = -20f, outHandleY = -50f, hasOutHandle = true),
            BezierPoint(x = 50f, y = -50f, inHandleX = 20f, inHandleY = -50f, hasInHandle = true),
            BezierPoint(x = 0f, y = 50f)
        )
        val vectorPath = VectorPathData(points = vectorPoints, isClosed = true, fillColor = 0xFF00FF00L)

        val originalDoc = ProjectDocument(
            name = "Victor Comprehensive Round Trip",
            canvasSize = CanvasSize(
                width = 1440,
                height = 1800,
                aspectRatio = AspectRatioPreset.CUSTOM,
                quality = QualityPreset.P1080
            ),
            editorSettings = EditorSettings(
                backgroundColor = 0xFF1E1E2EL, // Custom canvas color
                showGrid = true,
                gridSize = 40
            ),
            layers = listOf(
                Layer(
                    id = "shape_1",
                    name = "Rounded Card",
                    type = LayerType.SHAPE,
                    blendMode = BlendMode.OVERLAY,
                    transform = Transform2D(translationX = 15f, translationY = 30f, scaleX = 1.1f, scaleY = 1.1f),
                    shapeData = ShapeData(type = ShapeType.ROUNDED_RECTANGLE, cornerRadius = 32f, fillColor = 0xFF336699L)
                ),
                Layer(
                    id = "text_1",
                    name = "Headline Layer",
                    type = LayerType.TEXT,
                    textData = TextLayerData(text = "Salty Graphics Round Trip", fontSize = 64f, isBold = true, fontFamily = "CustomFont-Bold")
                ),
                Layer(
                    id = "vector_1",
                    name = "Curved Vector",
                    type = LayerType.VECTOR,
                    vectorPathData = vectorPath
                ),
                Layer(
                    id = "media_1",
                    name = "Background Photo",
                    type = LayerType.MEDIA,
                    mediaAssetPath = "assets/media_banner.jpg"
                )
            )
        )

        // Assets: media + imported font
        val mediaBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0x01, 0x02)
        val fontBytes = "fake-ttf-font-binary-data".toByteArray(Charsets.UTF_8)
        val assets = mapOf(
            "media_banner.jpg" to mediaBytes,
            "fonts/CustomFont-Bold.ttf" to fontBytes
        )

        // Action tree history
        val historySnapshot = ActionTreeSnapshot(
            rootNodeId = "root",
            currentNodeId = "node_2",
            nodes = mapOf(
                "root" to ActionNode(id = "root", description = "Initial Document", snapshot = originalDoc),
                "node_1" to ActionNode(id = "node_1", description = "Added Shape", parentId = "root", childrenIds = listOf("node_2")),
                "node_2" to ActionNode(id = "node_2", description = "Added Vector", parentId = "node_1")
            )
        )

        // Preview PNG bytes
        val previewBytes = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte(), 0x0D, 0x0A, 0x1A, 0x0A)

        // Export to stream
        val outStream = ByteArrayOutputStream()
        SgProjectArchive.exportToStream(
            document = originalDoc,
            outputStream = outStream,
            assetFiles = assets,
            historySnapshot = historySnapshot,
            previewPng = previewBytes
        )

        val zipBytes = outStream.toByteArray()
        assertTrue("Exported archive must be non-empty", zipBytes.isNotEmpty())

        // Re-import from stream
        val inStream = ByteArrayInputStream(zipBytes)
        val result = SgProjectArchive.importFromStream(inStream)

        // Verify Document Fidelity
        assertEquals(originalDoc.id, result.document.id)
        assertEquals(originalDoc.name, result.document.name)
        assertEquals(originalDoc.canvasSize.width, result.document.canvasSize.width)
        assertEquals(originalDoc.canvasSize.height, result.document.canvasSize.height)
        assertEquals(0xFF1E1E2EL, result.document.editorSettings.backgroundColor)
        assertEquals(4, result.document.layers.size)

        // Verify Shape Layer
        val shapeLayer = result.document.layers.find { it.id == "shape_1" }
        assertNotNull(shapeLayer)
        assertEquals(LayerType.SHAPE, shapeLayer?.type)
        assertEquals(BlendMode.OVERLAY, shapeLayer?.blendMode)
        assertEquals(32f, shapeLayer?.shapeData?.cornerRadius)
        assertEquals(0xFF336699L, shapeLayer?.shapeData?.fillColor)

        // Verify Text Layer
        val textLayer = result.document.layers.find { it.id == "text_1" }
        assertNotNull(textLayer)
        assertEquals("Salty Graphics Round Trip", textLayer?.textData?.text)
        assertEquals("CustomFont-Bold", textLayer?.textData?.fontFamily)

        // Verify Vector Layer
        val vectorLayer = result.document.layers.find { it.id == "vector_1" }
        assertNotNull(vectorLayer)
        assertEquals(3, vectorLayer?.vectorPathData?.points?.size)
        assertTrue(vectorLayer?.vectorPathData?.points?.get(0)?.hasOutHandle == true)

        // Verify Media Layer
        val mediaLayer = result.document.layers.find { it.id == "media_1" }
        assertNotNull(mediaLayer)
        assertEquals("assets/media_banner.jpg", mediaLayer?.mediaAssetPath)

        // Verify Assets Fidelity
        assertTrue(result.assets.containsKey("media_banner.jpg"))
        assertArrayEquals(mediaBytes, result.assets["media_banner.jpg"])
        assertTrue(result.assets.containsKey("fonts/CustomFont-Bold.ttf"))
        assertArrayEquals(fontBytes, result.assets["fonts/CustomFont-Bold.ttf"])

        // Verify History Fidelity
        assertNotNull(result.historySnapshot)
        assertEquals("root", result.historySnapshot?.rootNodeId)
        assertEquals("node_2", result.historySnapshot?.currentNodeId)
        assertEquals(3, result.historySnapshot?.nodes?.size)
        assertEquals("Added Vector", result.historySnapshot?.nodes?.get("node_2")?.description)

        // Verify Preview PNG
        assertNotNull(result.previewPng)
        assertArrayEquals(previewBytes, result.previewPng)
    }

    @Test
    fun testCorruptFileThrowsCleanly() {
        val badBytes = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05)
        try {
            SgProjectArchive.importFromStream(ByteArrayInputStream(badBytes))
            fail("Corrupted file must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue("Error must mention corrupted .sg file", e.message?.contains("Corrupted") == true)
        }
    }

    @Test
    fun testMissingProjectJsonThrowsCleanly() {
        val outStream = ByteArrayOutputStream()
        val zipOut = ZipOutputStream(outStream)
        zipOut.putNextEntry(ZipEntry("manifest.json"))
        zipOut.write("{\"formatVersion\": \"1.0.0\"}".toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
        zipOut.finish()

        try {
            SgProjectArchive.importFromStream(ByteArrayInputStream(outStream.toByteArray()))
            fail("Missing project.json must throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue("Error must state missing project.json", e.message?.contains("missing project.json") == true)
        }
    }

    @Test
    fun testUnsupportedNewerFormatVersionThrowsCleanly() {
        val outStream = ByteArrayOutputStream()
        val zipOut = ZipOutputStream(outStream)
        zipOut.putNextEntry(ZipEntry("manifest.json"))
        zipOut.write("{\"formatVersion\": \"2.5.0\"}".toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
        zipOut.putNextEntry(ZipEntry("project.json"))
        zipOut.write("{\"name\": \"Future Project\"}".toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
        zipOut.finish()

        try {
            SgProjectArchive.importFromStream(ByteArrayInputStream(outStream.toByteArray()))
            fail("Format version 2.5.0 must be rejected")
        } catch (e: IllegalArgumentException) {
            assertTrue("Error must explain unsupported format version", e.message?.contains("Unsupported .sg format version '2.5.0'") == true)
        }
    }
}
