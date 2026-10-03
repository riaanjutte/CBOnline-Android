package io.github.riaanjutte.cbonline.roster

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeOnMissionTest {

    @Test
    fun formats() {
        assertEquals("27 min", formatTimeOnMission("00:27"))
        assertEquals("2 min", formatTimeOnMission("00:02"))
        assertEquals("0 min", formatTimeOnMission("00:00"))
        assertEquals("1 h 05 min", formatTimeOnMission("01:05"))
        assertEquals("12 h 00 min", formatTimeOnMission("12:00"))
        assertEquals("123 h 05 min", formatTimeOnMission("123:05"))
    }

    @Test
    fun `unparseable is verbatim`() {
        assertEquals("", formatTimeOnMission(""))
        assertEquals("abc", formatTimeOnMission("abc"))
        assertEquals("1:5", formatTimeOnMission("1:5"))
    }
}
