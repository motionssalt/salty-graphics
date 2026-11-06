package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class LayerRegressionTest {

    @Test
    fun testAllLayerTypesRoundTrip() {
        val originalDoc = ProjectDocument(
            name = "All Layer Types Test",
            canvasSize = CanvasSize(width = 1080, height = 1920),
            layers = listOf(
                Layer(
                    id = "media_1",
                    name = "Media Layer",
                    type = LayerType.MEDIA,
                    mediaAssetPath = "assets/photo.png",
                    opacity = 0.9f
                ),
                Layer(
                    id = "text_1",
                    name = "Text Layer",
                    type = LayerType.TEXT,
                    textData = TextLayerData(
                        text = "Hello Salty World",
                        fontSize = 48f,
                        color = 0xFF00E5FFL,
                        isBold = true,
                        isItalic = false,
                        letterSpacing = 2f,
                        lineSpacing = 1.3f
                    )
                ),
                Layer(
                    id = "adj_1",
                    name = "Adjustment Layer",
                    type = LayerType.ADJUSTMENT,
                    blendMode = BlendMode.OVERLAY,
                    opacity = 0.5f
                ),
                Layer(
                    id = "shape_1",
                    name = "Shape Layer",
                    type = LayerType.SHAPE,
                    shapeData = ShapeData(
                        type = ShapeType.ROUNDED_RECTANGLE,
                        width = 400f,
                        height = 300f,
                        cornerRadius = 32f,
                        fillColor = 0xFFFFB300L,
                        strokeColor = 0xFFFFFFFFL,
                        strokeWidth = 4f
                    )
                ),
                Layer(
                    id = "vector_1",
                    name = "Vector Layer",
                    type = LayerType.VECTOR,
                    vectorPathData = VectorPathData(
                        points = listOf(
                            BezierPoint(0f, 0f),
                            BezierPoint(100f, 0f),
                            BezierPoint(100f, 100f)
                        ),
                        isClosed = true,
                        strokeColor = 0xFF00E5FFL,
                        fillColor = 0x882979FFL
                    )
                ),
                Layer(
                    id = "group_1",
                    name = "Group Layer",
                    type = LayerType.GROUP,
                    children = listOf(
                        Layer(
                            id = "child_shape",
                            name = "Child Shape",
                            type = LayerType.SHAPE,
                            shapeData = ShapeData(type = ShapeType.CIRCLE, fillColor = 0xFFFF5252L)
                        )
                    )
                )
            )
        )

        val outStream = ByteArrayOutputStream()
        SgProjectArchive.exportToStream(originalDoc, outStream)

        val inStream = ByteArrayInputStream(outStream.toByteArray())
        val (reloadedDoc, _) = SgProjectArchive.importFromStream(inStream)

        assertEquals("Project name should match", originalDoc.name, reloadedDoc.name)
        assertEquals("Layer count should match", 6, reloadedDoc.layers.size)

        // Verify each layer type preserved
        val media = reloadedDoc.layers.find { it.id == "media_1" }
        assertNotNull(media)
        assertEquals(LayerType.MEDIA, media?.type)

        val text = reloadedDoc.layers.find { it.id == "text_1" }
        assertNotNull(text)
        assertEquals(LayerType.TEXT, text?.type)
        assertEquals("Hello Salty World", text?.textData?.text)
        assertEquals(0xFF00E5FFL, text?.textData?.color)

        val adj = reloadedDoc.layers.find { it.id == "adj_1" }
        assertNotNull(adj)
        assertEquals(LayerType.ADJUSTMENT, adj?.type)

        val shape = reloadedDoc.layers.find { it.id == "shape_1" }
        assertNotNull(shape)
        assertEquals(LayerType.SHAPE, shape?.type)
        assertEquals(0xFFFFB300L, shape?.shapeData?.fillColor)
        assertEquals(32f, shape?.shapeData?.cornerRadius)

        val vector = reloadedDoc.layers.find { it.id == "vector_1" }
        assertNotNull(vector)
        assertEquals(LayerType.VECTOR, vector?.type)
        assertEquals(3, vector?.vectorPathData?.points?.size)

        val group = reloadedDoc.layers.find { it.id == "group_1" }
        assertNotNull(group)
        assertEquals(LayerType.GROUP, group?.type)
        assertEquals(1, group?.children?.size)
        assertEquals(LayerType.SHAPE, group?.children?.firstOrNull()?.type)
    }

    @Test
    fun testCorruptedLayerIsQuarantinedWithoutCrashing() {
        val corruptedProjectJson = """
        {
            "id": "corrupted-test-doc",
            "name": "Project with one bad layer",
            "canvasSize": { "width": 1080, "height": 1920 },
            "layers": [
                {
                    "id": "good-layer",
                    "name": "Healthy Text",
                    "type": "TEXT",
                    "textData": { "text": "Valid Text", "color": 4278257151 }
                },
                {
                    "id": "bad-layer",
                    "name": "Broken Layer",
                    "type": "UNKNOWN_FUTURE_TYPE_DOES_NOT_EXIST",
                    "shapeData": "this is totally corrupt"
                }
            ]
        }
        """.trimIndent()

        // Create zip archive with corrupted project.json
        val outStream = ByteArrayOutputStream()
        val zipOut = java.util.zip.ZipOutputStream(outStream)
        zipOut.putNextEntry(java.util.zip.ZipEntry("project.json"))
        zipOut.write(corruptedProjectJson.toByteArray(Charsets.UTF_8))
        zipOut.closeEntry()
        zipOut.finish()

        // Import should NOT throw exception and quarantine the corrupted layer
        val inStream = ByteArrayInputStream(outStream.toByteArray())
        val (doc, _) = SgProjectArchive.importFromStream(inStream)

        assertNotNull(doc)
        assertEquals("corrupted-test-doc", doc.id)
        assertEquals(2, doc.layers.size)

        val goodLayer = doc.layers.find { it.id == "good-layer" }
        assertNotNull(goodLayer)
        assertEquals(LayerType.TEXT, goodLayer?.type)
        assertEquals("Valid Text", goodLayer?.textData?.text)

        val quarantinedLayer = doc.layers.find { it.id == "bad-layer" }
        assertNotNull(quarantinedLayer)
        assertEquals(LayerType.QUARANTINED, quarantinedLayer?.type)
        assertTrue(quarantinedLayer?.name?.contains("Quarantined") == true)
        assertNotNull(quarantinedLayer?.quarantinedData)
        assertTrue(quarantinedLayer?.quarantinedData?.rawJson?.contains("bad-layer") == true)
    }
}
