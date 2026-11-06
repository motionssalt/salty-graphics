package com.motionssalt.saltygraphics

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.motionssalt.saltygraphics.data.FontItem
import com.motionssalt.saltygraphics.data.FontManager
import com.motionssalt.saltygraphics.data.FontOrigin
import com.motionssalt.saltygraphics.engine.file.SgProjectArchive
import com.motionssalt.saltygraphics.engine.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FontManagerTest {

    private lateinit var context: android.content.Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Clean any existing test fonts
        val dir = File(context.filesDir, "custom_fonts")
        if (dir.exists()) dir.deleteRecursively()
    }

    @Test
    fun testBuiltInFontsAndDeviceDefault() {
        val builtIns = FontManager.getBuiltInFonts()
        assertTrue("Must bundle built-in open-license fonts", builtIns.size >= 7)

        val families = builtIns.map { it.familyName }
        assertTrue("Must include Inter", families.contains("Inter"))
        assertTrue("Must include Montserrat", families.contains("Montserrat"))
        assertTrue("Must include Playfair Display", families.contains("Playfair Display"))
        assertTrue("Must include Oswald", families.contains("Oswald"))
        assertTrue("Must include Dancing Script", families.contains("Dancing Script"))
        assertTrue("Must include JetBrains Mono", families.contains("JetBrains Mono"))
        assertTrue("Must include DejaVu Serif", families.contains("DejaVu Serif"))

        val allFonts = FontManager.getAllFonts(context)
        assertEquals("Device default must be first", FontManager.DEVICE_DEFAULT, allFonts.first())
    }

    @Test
    fun testFontImportValidationAndDuplicateDetection() {
        // Create 1 valid TTF dummy file with valid header (0x00010000 + 1024 bytes)
        val validTtfFile = File(context.cacheDir, "TestFont-Regular.ttf")
        FileOutputStream(validTtfFile).use { fos ->
            fos.write(byteArrayOf(0x00, 0x01, 0x00, 0x00))
            fos.write(ByteArray(2048) { 0x41 })
        }

        // Create 1 corrupt non-font file
        val corruptFile = File(context.cacheDir, "corrupt.txt")
        corruptFile.writeText("This is not a font")

        // Create 1 invalid format file
        val wrongFormatFile = File(context.cacheDir, "image.png")
        wrongFormatFile.writeBytes(ByteArray(1500) { 0x00 })

        val uris = listOf(
            Uri.fromFile(validTtfFile),
            Uri.fromFile(corruptFile),
            Uri.fromFile(wrongFormatFile)
        )

        val summary = FontManager.importFontsFromUris(context, uris)
        assertEquals(3, summary.totalProcessed)
        assertEquals("Valid TTF font must be imported", 1, summary.importedCount)
        assertEquals("Invalid files must be skipped", 2, summary.skippedCount)
        assertTrue(summary.skippedReasons.any { it.contains("Unsupported format") })

        val imported = FontManager.getImportedFonts(context)
        assertEquals(1, imported.size)
        assertEquals("TestFont", imported.first().familyName)
        assertEquals("Regular", imported.first().styleName)

        // Try importing the exact same font again -> must detect duplicate and skip
        val dupSummary = FontManager.importFontsFromUris(context, listOf(Uri.fromFile(validTtfFile)))
        assertEquals(0, dupSummary.importedCount)
        assertEquals(1, dupSummary.skippedCount)
        assertTrue(dupSummary.skippedReasons.first().contains("Already installed"))

        // Delete font
        val deleted = FontManager.deleteImportedFont(context, imported.first().id)
        assertTrue("Should successfully delete imported font", deleted)
        assertTrue("Imported font list should now be empty", FontManager.getImportedFonts(context).isEmpty())
    }

    @Test
    fun testTextLayerDataRoundTripWithAllTextOptions() {
        val originalText = TextLayerData(
            text = "Salty Graphics Rule!",
            fontFamily = "Montserrat",
            fontStyleName = "Bold",
            fontSize = 84f,
            color = 0xFFFF0055L,
            justification = TextJustification.CENTER,
            alignment = TextAlignment.MIDDLE,
            letterSpacing = 50f,
            lineSpacing = 140f,
            isBold = true,
            isItalic = false,
            hasOutline = true,
            outlineColor = 0xFFFFFFFFL,
            outlineWidth = 4f,
            boxMode = TextBoxMode.PARAGRAPH,
            boxWidth = 600f,
            boxHeight = 300f,
            followPathLayerId = "path_layer_123",
            pathOffsetPercent = 0.35f
        )

        val doc = ProjectDocument(
            name = "Text Options Test",
            layers = listOf(
                Layer(name = "Text 1", type = LayerType.TEXT, textData = originalText)
            )
        )

        val bos = ByteArrayOutputStream()
        SgProjectArchive.exportToStream(doc, bos)
        val bis = ByteArrayInputStream(bos.toByteArray())
        val (restoredDoc, _) = SgProjectArchive.importFromStream(bis)

        val restoredText = restoredDoc.layers.first().textData
        assertNotNull(restoredText)
        assertEquals(originalText, restoredText)
    }
}
