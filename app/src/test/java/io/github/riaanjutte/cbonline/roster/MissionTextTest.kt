package io.github.riaanjutte.cbonline.roster

import io.github.riaanjutte.cbonline.data.Weather
import io.github.riaanjutte.cbonline.data.Wind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.Locale

class MissionTextTest {

    private val end = Instant.parse("2026-10-04T00:42:00Z")

    /** Time-left label when [d] remains; a negative [d] means we're past the end. */
    private fun left(d: Duration) = timeLeftLabel(end, end.minus(d))

    private fun weather(
        temp: Double? = 14.0,
        cover: String? = "Medium",
        base: Int? = 2800,
        top: Int? = 4300,
        precipLevel: Double = 0.0,
        precipType: String? = null,
        wind: Wind? = Wind(210, 3.0)
    ) = Weather(temp, cover, base, top, precipLevel, precipType, wind)

    @Test
    fun `hours and minutes`() {
        assertEquals("2 h 47 min left", left(Duration.ofHours(2).plusMinutes(47).plusSeconds(30)))
        assertEquals("2 h 05 min left", left(Duration.ofHours(2).plusMinutes(5)))
        assertEquals("1 h 00 min left", left(Duration.ofHours(1)))
    }

    @Test
    fun `minutes only`() {
        assertEquals("59 min left", left(Duration.ofMinutes(59).plusSeconds(59)))
        assertEquals("47 min left", left(Duration.ofMinutes(47)))
        assertEquals("1 min left", left(Duration.ofMinutes(1)))
    }

    @Test
    fun `under a minute`() {
        assertEquals("Less than a minute left", left(Duration.ofSeconds(59)))
        assertEquals("Less than a minute left", left(Duration.ofSeconds(1)))
    }

    @Test
    fun `changing mission`() {
        assertEquals("Changing mission…", left(Duration.ZERO))
        assertEquals("Changing mission…", left(Duration.ofMinutes(-5)))
        assertEquals("Changing mission…", left(Duration.ofMinutes(-10)))
    }

    @Test
    fun `out of date`() {
        assertEquals("Mission info may be out of date", left(Duration.ofMinutes(-10).minusSeconds(1)))
        assertEquals("Mission info may be out of date", left(Duration.ofHours(-3)))
    }

    @Test
    fun `full line`() {
        assertEquals(
            "1 °C · Heavy cloud 1,900–4,900 m · Wind 160° 3 m/s",
            weatherLine(Weather(1.0, "Heavy", 1900, 4900, 0.0, null, Wind(160, 3.0)))
        )
    }

    @Test
    fun `clear sky variants`() {
        for (cover in listOf("Clear", "clear", null, "  ")) {
            assertEquals("cover=$cover", "14 °C · Clear sky · Wind 210° 3 m/s", weatherLine(weather(cover = cover)))
        }
    }

    @Test
    fun `unknown cloud cover is shown verbatim`() {
        assertEquals(
            "14 °C · Overcast cloud 800–2,000 m · Wind 210° 3 m/s",
            weatherLine(weather(cover = "Overcast", base = 800, top = 2000))
        )
    }

    @Test
    fun `cover without heights`() {
        assertEquals("14 °C · Medium cloud · Wind 210° 3 m/s", weatherLine(weather(base = null, top = null)))
    }

    @Test
    fun `precipitation`() {
        assertTrue(weatherLine(weather(precipLevel = 0.5, precipType = "snow")).endsWith(" · Snow"))
        assertTrue(weatherLine(weather(precipLevel = 0.5, precipType = null)).endsWith(" · Precipitation"))
        assertEquals(
            "14 °C · Medium cloud 2,800–4,300 m · Wind 210° 3 m/s",
            weatherLine(weather(precipLevel = 0.0, precipType = "rain"))
        )
    }

    @Test
    fun `missing parts are omitted`() {
        assertEquals("14 °C · Medium cloud 2,800–4,300 m", weatherLine(weather(wind = null)))
        assertEquals("Medium cloud 2,800–4,300 m · Wind 210° 3 m/s", weatherLine(weather(temp = null)))
    }

    @Test
    fun `negative and fractional numbers`() {
        assertTrue(weatherLine(weather(temp = -15.0)).startsWith("-15 °C · "))
        assertTrue(weatherLine(weather(temp = -2.5)).startsWith("-2.5 °C · "))
        assertTrue(weatherLine(weather(wind = Wind(210, 3.5))).contains("Wind 210° 3.5 m/s"))
    }

    @Test
    fun `grouping ignores the phone locale`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val line = weatherLine(Weather(1.0, "Heavy", 1900, 4900, 0.0, null, Wind(160, 3.5)))
            assertEquals("1 °C · Heavy cloud 1,900–4,900 m · Wind 160° 3.5 m/s", line)
            assertFalse(line.contains("1.900"))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `in-game label`() {
        assertEquals("2 Dec 1944, 14:00", inGameLabel(LocalDateTime.of(1944, 12, 2, 14, 0)))
        assertEquals("15 Sep 1943, 11:00", inGameLabel(LocalDateTime.of(1943, 9, 15, 11, 0)))
    }
}
