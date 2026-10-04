package io.github.riaanjutte.cbonline.notify

import io.github.riaanjutte.cbonline.data.NextMission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReminderPlannerTest {

    private val now = Instant.parse("2026-10-04T12:00:00Z")
    private fun at(minutesFromNow: Long) = now.plusSeconds(minutesFromNow * 60)
    private fun next(name: String, startMinutes: Long) = NextMission(name = name, expectedStart = at(startMinutes), historicalStart = null, weather = null)

    @Test
    fun `reminds ten minutes before the start`() =
        assertEquals(at(20), MissionReminder("Paravane", at(30)).remindAt)

    @Test
    fun `a reminder is possible only while the start is more than ten minutes away`() {
        assertTrue(canRemind(at(11), now))
        assertFalse(canRemind(at(10), now))
        assertFalse(canRemind(at(5), now))
    }

    @Test
    fun `same mission, same start keeps the reminder`() =
        assertEquals(ReminderAction.Keep, reconcile(MissionReminder("Paravane", at(30)), next("Paravane", 30), now))

    @Test
    fun `a moved start moves the reminder`() =
        assertEquals(
            ReminderAction.Move(MissionReminder("Paravane", at(45))),
            reconcile(MissionReminder("Paravane", at(30)), next("Paravane", 45), now)
        )

    @Test
    fun `a different next mission drops the reminder`() =
        assertEquals(ReminderAction.Drop, reconcile(MissionReminder("Paravane", at(30)), next("Bodenplatte", 30), now))

    @Test
    fun `a mission that has started drops the reminder`() =
        assertEquals(ReminderAction.Drop, reconcile(MissionReminder("Paravane", at(-1)), null, now))

    @Test
    fun `a reminder whose time just passed is kept so a late alarm still fires`() =
        assertEquals(ReminderAction.Keep, reconcile(MissionReminder("Paravane", at(5)), next("Paravane", 5), now))

    @Test
    fun `no next mission in the feed keeps the reminder`() =
        assertEquals(ReminderAction.Keep, reconcile(MissionReminder("Paravane", at(30)), null, now))
}
