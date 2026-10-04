package io.github.riaanjutte.cbonline.ui.brand

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeptValueTest {

    @Test
    fun `makes the value once and then hands back the same one`() {
        val kept = KeptValue<String>()
        var made = 0
        assertNull(kept.value)
        assertEquals("map", kept.getOrMake { made++; "map" })
        assertEquals("map", kept.getOrMake { made++; "other" })
        assertEquals("map", kept.value)
        assertEquals(1, made)
    }

    @Test
    fun `a failed attempt is tried again next time`() {
        val kept = KeptValue<String>()
        assertNull(kept.getOrMake { null })
        assertEquals("map", kept.getOrMake { "map" })
    }
}
