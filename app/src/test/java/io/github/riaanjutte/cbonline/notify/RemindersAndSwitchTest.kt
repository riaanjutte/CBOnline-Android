package io.github.riaanjutte.cbonline.notify

import io.github.riaanjutte.cbonline.data.NextMission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class RemindersAndSwitchTest {

    private val now = Instant.parse("2026-10-04T12:00:00Z")
    private fun at(minutes: Long) = now.plusSeconds(minutes * 60)
    private fun next(name: String, start: Long) = NextMission(name, at(start), null, null)

    private class FakeReminderStore : ReminderRepository {
        override val reminder = MutableStateFlow<MissionReminder?>(null)
        override suspend fun set(r: MissionReminder) { reminder.value = r }
        override suspend fun clear() { reminder.value = null }
    }

    private class FakeAlarms : ReminderAlarms {
        val log = mutableListOf<String>()
        override fun schedule(r: MissionReminder) { log += "schedule ${r.missionName} ${r.start}" }
        override fun cancel() { log += "cancel" }
    }

    private val store = FakeReminderStore()
    private val alarms = FakeAlarms()
    private val reminders = MissionReminders(store, alarms)

    @Test
    fun `toggling sets a reminder for the next mission, toggling again cancels it`() = runTest {
        reminders.toggle(next("Paravane", 30), now)
        assertEquals(MissionReminder("Paravane", at(30)), store.reminder.value)
        reminders.toggle(next("Paravane", 30), now)
        assertNull(store.reminder.value)
        assertEquals(listOf("schedule Paravane ${at(30)}", "cancel"), alarms.log)
    }

    @Test
    fun `a tap in the last ten minutes still sets a reminder, which then comes straight away`() = runTest {
        reminders.toggle(next("Paravane", 5), now)
        assertEquals(MissionReminder("Paravane", at(5)), store.reminder.value)
    }

    @Test
    fun `no reminder once the mission has started`() = runTest {
        reminders.toggle(next("Paravane", -1), now)
        assertNull(store.reminder.value)
        assertEquals(emptyList<String>(), alarms.log)
    }

    @Test
    fun `a moved start reschedules, a different mission cancels`() = runTest {
        reminders.toggle(next("Paravane", 30), now)
        reminders.reconcile(next("Paravane", 45), now)
        assertEquals(MissionReminder("Paravane", at(45)), store.reminder.value)
        reminders.reconcile(next("Bodenplatte", 45), now)
        assertNull(store.reminder.value)
        assertEquals(listOf("schedule Paravane ${at(30)}", "schedule Paravane ${at(45)}", "cancel"), alarms.log)
    }

    @Test
    fun `a kept reminder re-arms its alarm, which Android drops if the app is force-stopped`() = runTest {
        reminders.toggle(next("Paravane", 30), now)
        reminders.reconcile(next("Paravane", 30), now)
        assertEquals(listOf("schedule Paravane ${at(30)}", "schedule Paravane ${at(30)}"), alarms.log)
    }

    @Test
    fun `reconciling with nothing set does nothing`() = runTest {
        reminders.reconcile(next("Paravane", 30), now)
        assertEquals(emptyList<String>(), alarms.log)
    }

    // Friend alerts on/off

    private class FakeAlertsStore : FriendAlertsRepository {
        override val enabled = MutableStateFlow(false)
        override val lastOnline = MutableStateFlow<Set<String>?>(setOf("stale"))
        override suspend fun setEnabled(on: Boolean) { enabled.value = on }
        override suspend fun setLastOnline(keys: Set<String>?) { lastOnline.value = keys }
    }

    private class FakeScheduler : AlertScheduler {
        val log = mutableListOf<String>()
        override fun start() { log += "start" }
        override fun stop() { log += "stop" }
    }

    @Test
    fun `switching alerts on remembers who's online now so they don't alert, and starts the checks`() = runTest {
        val alerts = FakeAlertsStore()
        val scheduler = FakeScheduler()
        val switch = FriendAlertSwitch(alerts, scheduler)
        switch.set(true, onlineNow = setOf("hans"))
        assertEquals(true, switch.enabled.first())
        assertEquals(setOf("hans"), alerts.lastOnline.value)
        switch.set(false, onlineNow = setOf("hans"))
        assertEquals(false, alerts.enabled.value)
        assertNull(alerts.lastOnline.value)
        assertEquals(listOf("start", "stop"), scheduler.log)
    }

    @Test
    fun `switching on before the roster has loaded leaves no list, so the first check only records it`() = runTest {
        val alerts = FakeAlertsStore()
        FriendAlertSwitch(alerts, FakeScheduler()).set(true, onlineNow = null)
        assertNull(alerts.lastOnline.value)
    }

    @Test
    fun `on app start the checks are restarted if alerts are on, and left alone if off`() = runTest {
        val alerts = FakeAlertsStore()
        val scheduler = FakeScheduler()
        val switch = FriendAlertSwitch(alerts, scheduler)
        switch.ensureRunning()
        alerts.enabled.value = true
        switch.ensureRunning()
        assertEquals(listOf("start"), scheduler.log)
    }
}
