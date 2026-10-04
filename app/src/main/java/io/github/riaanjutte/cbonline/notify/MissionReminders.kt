package io.github.riaanjutte.cbonline.notify

import io.github.riaanjutte.cbonline.data.NextMission
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant

/** How long before a mission starts the reminder comes. */
val REMINDER_LEAD: Duration = Duration.ofMinutes(10)

/** A reminder for one upcoming mission, identified by its name. */
data class MissionReminder(val missionName: String, val start: Instant) {
    val remindAt: Instant get() = start.minus(REMINDER_LEAD)
}

/** The bell only offers a reminder while there's still time for it to come before the start. */
fun canRemind(start: Instant, now: Instant): Boolean = start.minus(REMINDER_LEAD).isAfter(now)

/** However late Android delivers the alarm, a reminder is never shown once its mission has started. */
fun shouldShowReminder(start: Instant, now: Instant): Boolean = now.isBefore(start)

sealed interface ReminderAction {
    data object Keep : ReminderAction
    data object Drop : ReminderAction
    data class Move(val reminder: MissionReminder) : ReminderAction
}

/**
 * What to do with a saved reminder once the mission feed has been read again. A reminder whose time has just
 * passed is kept until the mission starts, so an alarm Android delivered late still fires.
 */
fun reconcile(saved: MissionReminder, next: NextMission?, now: Instant): ReminderAction = when {
    !saved.start.isAfter(now) -> ReminderAction.Drop
    next == null -> ReminderAction.Keep
    next.name != saved.missionName -> ReminderAction.Drop
    next.expectedStart != saved.start -> ReminderAction.Move(MissionReminder(saved.missionName, next.expectedStart))
    else -> ReminderAction.Keep
}

interface ReminderRepository {
    val reminder: Flow<MissionReminder?>
    suspend fun set(r: MissionReminder)
    suspend fun clear()
}

/** Android's alarm for the one pending reminder. */
interface ReminderAlarms {
    fun schedule(r: MissionReminder)
    fun cancel()
}

/** The bell on the mission card: one reminder at a time, kept in step with the mission feed. */
class MissionReminders(private val store: ReminderRepository, private val alarms: ReminderAlarms) {

    val reminder: Flow<MissionReminder?> = store.reminder

    /**
     * Sets a reminder for [next], or cancels it if one is already set for that mission. A tap that lands in the
     * last ten minutes (the bell hides on a 30-second tick) still sets one, which then comes straight away.
     */
    suspend fun toggle(next: NextMission, now: Instant) {
        if (store.reminder.first()?.missionName == next.name) {
            cancel()
        } else if (next.expectedStart.isAfter(now)) {
            val r = MissionReminder(next.name, next.expectedStart)
            store.set(r)
            alarms.schedule(r)
        }
    }

    suspend fun reconcile(next: NextMission?, now: Instant) {
        val saved = store.reminder.first() ?: return
        when (val action = reconcile(saved, next, now)) {
            // Re-arm: Android drops an app's alarms when it's force-stopped. Same request, so it replaces itself
            ReminderAction.Keep -> alarms.schedule(saved)
            ReminderAction.Drop -> cancel()
            is ReminderAction.Move -> {
                store.set(action.reminder)
                alarms.schedule(action.reminder)
            }
        }
    }

    private suspend fun cancel() {
        store.clear()
        alarms.cancel()
    }
}
