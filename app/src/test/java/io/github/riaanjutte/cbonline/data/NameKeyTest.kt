package io.github.riaanjutte.cbonline.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class NameKeyTest {

    @Test
    fun `ignores case and surrounding spaces`() = assertEquals("[cb]hans", nameKey("  [CB]Hans "))

    @Test
    fun `doesn't depend on the phone's language`() {
        val saved = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("tr")) // Turkish lower-cases "I" to a dotless "ı"
            assertEquals("ivan", nameKey("IVAN"))
        } finally {
            Locale.setDefault(saved)
        }
    }
}
