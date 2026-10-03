package io.github.riaanjutte.cbonline.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionsTest {

    @Test
    fun compares() {
        assertTrue(isNewer("1.0.10", "1.0.9"))
        assertTrue(isNewer("1.1", "1.0.9"))
        assertTrue(isNewer("2.0.0", "1.9.9"))
        assertFalse(isNewer("1.0", "1.0.0"))
        assertFalse(isNewer("1.0.0", "1.0"))
        assertFalse(isNewer("1.0.9", "1.0.10"))
    }

    @Test
    fun `non-numeric segments are never newer`() {
        assertFalse(isNewer("1.1.0-beta", "1.0.0"))
        assertFalse(isNewer("2.0.0", "dev"))
    }
}
