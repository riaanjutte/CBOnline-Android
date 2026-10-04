package io.github.riaanjutte.cbonline.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.riaanjutte.cbonline.CbOnlineApp
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.concurrent.TimeUnit

private const val ACTION_REMIND = "io.github.riaanjutte.cbonline.action.REMIND"
private const val EXTRA_NAME = "mission"
private const val EXTRA_START = "start"

/**
 * The reminder alarm. Exact where Android allows it (always before Android 12; from 12 on, once the user has
 * switched on "Alarms & reminders" for the app), so it comes on time even when the phone is asleep. Otherwise
 * it falls back to an inexact alarm, which Android may deliver well after the time; the receiver then drops
 * a reminder whose mission has already started.
 */
class AlarmReminderAlarms(private val context: Context) : ReminderAlarms {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(r: MissionReminder) {
        val at = r.remindAt.toEpochMilli()
        if (canScheduleExact(alarmManager)) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(r))
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent(r))
        }
    }

    override fun cancel() = alarmManager.cancel(intent(null))

    companion object {
        /** True when exact alarms need the user to switch on "Alarms & reminders" first (Android 12+). */
        fun needsExactAlarmAccess(context: Context): Boolean =
            !canScheduleExact(context.getSystemService(AlarmManager::class.java))

        private fun canScheduleExact(alarmManager: AlarmManager): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    /** Extras don't take part in matching, so one request code and action always mean "the" reminder. */
    private fun intent(r: MissionReminder?): PendingIntent = PendingIntent.getBroadcast(
        context, 0,
        Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND).apply {
            r?.let { putExtra(EXTRA_NAME, it.missionName); putExtra(EXTRA_START, it.start.toEpochMilli()) }
        },
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}

/** Shows the reminder when its alarm goes off. Not exported: only the app's own alarm can trigger it. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REMIND) return
        val name = intent.getStringExtra(EXTRA_NAME) ?: return
        val start = Instant.ofEpochMilli(intent.getLongExtra(EXTRA_START, 0))
        // An inexact alarm can come very late; a reminder after the start would only mislead
        if (shouldShowReminder(start, Instant.now())) Notifications.showMissionReminder(context, name, start)
        val store = (context.applicationContext as CbOnlineApp).container.reminderStore
        inBackground {
            // Clear only this reminder: a newer one may have replaced it meanwhile
            if (store.reminder.first()?.missionName == name) store.clear()
        }
    }
}

/**
 * Re-arms the pending reminder when the phone restarts (which drops alarms), after an app update (harmless if
 * the alarm survived) and when the user switches on "Alarms & reminders" (so it becomes an exact alarm).
 * Exported for those system broadcasts, which other apps can't send.
 */
class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESTART_ACTIONS) return
        val container = (context.applicationContext as CbOnlineApp).container
        inBackground {
            val saved = container.reminderStore.reminder.first() ?: return@inBackground
            if (saved.start.isAfter(Instant.now())) container.reminderAlarms.schedule(saved)
            else container.reminderStore.clear()
        }
    }
}

private val RESTART_ACTIONS = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED
)

/**
 * Runs [work] off the main thread while keeping the receiver alive until it's done. A failed settings write
 * (disk full, corrupt file) is logged, not allowed to crash the app, as in the view model.
 */
private fun BroadcastReceiver.inBackground(work: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            work()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CBOnline", "Reminder bookkeeping failed", e)
        } finally {
            pending.finish()
        }
    }
}

/** Runs the friend check about every 15 minutes (Android's minimum), only with a network connection. */
class WorkManagerAlertScheduler(private val context: Context) : AlertScheduler {

    override fun start() {
        val request = PeriodicWorkRequestBuilder<FriendAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    override fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    companion object {
        const val WORK_NAME = "friend-alerts"
    }
}

class FriendAlertWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as CbOnlineApp
        app.container.friendAlertCheck(inForeground = { app.inForeground }).run()
        // A failed fetch just waits for the next period; retrying sooner would only cost battery
        return Result.success()
    }
}
