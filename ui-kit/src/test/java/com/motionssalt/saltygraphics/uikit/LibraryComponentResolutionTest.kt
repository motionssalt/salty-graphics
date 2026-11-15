package com.motionssalt.saltygraphics.uikit

import com.motionssalt.saltygraphics.uikit.registry.LibraryComponentEntry
import com.motionssalt.saltygraphics.uikit.registry.LibraryComponentRegistry
import org.junit.Assert.*
import org.junit.Test

class LibraryComponentResolutionTest {
    @Test fun requiredResolutionFailsAfterRemovalInsteadOfSilentlyHidingAControl() {
        val id = "required_resolution_test"
        val entry = LibraryComponentEntry<String>(id = id) { _, _, _, _ -> }
        try {
            LibraryComponentRegistry.register(entry)
            assertSame(entry, LibraryComponentRegistry.resolveRequired(id))
            LibraryComponentRegistry.unregister(id)
            val failure = assertThrows(IllegalArgumentException::class.java) {
                LibraryComponentRegistry.resolveRequired(id)
            }
            assertTrue(failure.message.orEmpty().contains(id))
        } finally {
            LibraryComponentRegistry.unregister(id)
        }
    }
}
