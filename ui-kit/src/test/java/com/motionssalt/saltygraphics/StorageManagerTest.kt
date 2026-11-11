package com.motionssalt.saltygraphics

import com.motionssalt.saltygraphics.data.StorageManager
import org.junit.Assert.*
import org.junit.Test

class StorageManagerTest {

    @Test
    fun testFilenameCollisionResolution() {
        val existing = setOf(
            "banner.png",
            "banner_1.png",
            "banner_2.png",
            "poster.jpg"
        )

        // Non-colliding name returns unchanged
        assertEquals("hero.png", StorageManager.resolveUniqueFileName(existing, "hero.png"))

        // Colliding name appends incrementing counter
        assertEquals("banner_3.png", StorageManager.resolveUniqueFileName(existing, "banner.png"))
        assertEquals("poster_1.jpg", StorageManager.resolveUniqueFileName(existing, "poster.jpg"))

        // Case insensitivity
        assertEquals("banner_3.png", StorageManager.resolveUniqueFileName(existing, "BANNER.png"))
    }
}
