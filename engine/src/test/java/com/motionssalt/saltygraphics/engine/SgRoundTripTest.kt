package com.motionssalt.saltygraphics.engine

import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class SgRoundTripTest {

    @Test
    fun testSgArchiveRoundTrip() {
        val originalDoc = ProjectDocument(
            name = "Test Project Export",
            canvasSize = CanvasSize(
                width = 1080,
                height = 1920,
                aspectRatio = AspectRatioPreset.STORY_9_16,
                quality = QualityPreset.P1080
            ),
            layers = listOf(
                Layer(
                    id = "layer1",
                    name = "Background Card",
                    type = LayerType.SHAPE,
                    blendMode = BlendMode.OVERLAY,
                    transform = Transform2D(translationX = 10f, translationY = 20f, scaleX = 1.2f, scaleY = 1.2f),
                    shapeData = ShapeData(type = ShapeType.ROUNDED_RECTANGLE, cornerRadius = 40f, fillColor = 0xFF123456)
                ),
                Layer(
                    id = "layer2",
                    name = "Headline",
                    type = LayerType.TEXT,
                    textData = TextLayerData(text = "Victor Salty Graphics", fontSize = 72f, isBold = true)
                )
            )
        )

        val assetData = "test font or media binary data".toByteArray(Charsets.UTF_8)
        val dummyAssets = mapOf("sample_media.png" to assetData)

        // Export to stream
        val outStream = ByteArrayOutputStream()
        SgProjectArchive.exportToStream(originalDoc, outStream, dummyAssets)
        val zipBytes = outStream.toByteArray()
        assertTrue("Exported ZIP archive should have non-zero size", zipBytes.isNotEmpty())

        // Re-import from stream
        val inStream = ByteArrayInputStream(zipBytes)
        val (importedDoc, importedAssets) = SgProjectArchive.importFromStream(inStream)

        // Verify document fidelity
        assertEquals(originalDoc.id, importedDoc.id)
        assertEquals(originalDoc.name, importedDoc.name)
        assertEquals(originalDoc.canvasSize.width, importedDoc.canvasSize.width)
        assertEquals(originalDoc.canvasSize.height, importedDoc.canvasSize.height)
        assertEquals(2, importedDoc.layers.size)

        val layer1 = importedDoc.layers.find { it.id == "layer1" }
        assertNotNull(layer1)
        assertEquals(BlendMode.OVERLAY, layer1?.blendMode)
        assertEquals(40f, layer1?.shapeData?.cornerRadius)
        assertEquals(0xFF123456L, layer1?.shapeData?.fillColor)

        val layer2 = importedDoc.layers.find { it.id == "layer2" }
        assertNotNull(layer2)
        assertEquals("Victor Salty Graphics", layer2?.textData?.text)
        assertTrue(layer2?.textData?.isBold == true)

        // Verify assets fidelity
        assertTrue(importedAssets.containsKey("sample_media.png"))
        assertArrayEquals(assetData, importedAssets["sample_media.png"])
    }
}
