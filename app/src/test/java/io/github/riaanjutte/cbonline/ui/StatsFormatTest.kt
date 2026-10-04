package io.github.riaanjutte.cbonline.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StatsFormatTest {

    private val us = StatsFormat(Locale.US)

    @Test
    fun `halves round up, as people expect`() {
        assertEquals("12.3", us.oneDecimal(12.25))
        assertEquals("0.1", us.oneDecimal(0.05))
        // 2.125 is exact in binary, so this tells half-up (2.13) from half-even (2.12)
        assertEquals("2.13", us.twoDecimals(2.125))
    }

    @Test
    fun `whole numbers are grouped and decimals keep their places`() {
        assertEquals("98,765", us.whole(98765))
        assertEquals("2.70", us.twoDecimals(2.7))
        assertEquals("60.0", us.oneDecimal(60.0))
    }

    @Test
    fun `the phone's locale decides separators`() {
        val de = StatsFormat(Locale.GERMANY)
        assertEquals("98.765", de.whole(98765))
        assertEquals("12,3", de.oneDecimal(12.25))
    }
}
