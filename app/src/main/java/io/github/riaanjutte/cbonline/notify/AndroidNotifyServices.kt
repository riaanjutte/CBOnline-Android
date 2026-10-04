package io.github.riaanjutte.cbonline.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.riaanjutte.cbonline.CbOnlineApp
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
 * The reminder alarm. Inexact on purpose: exact alarms need a permission Android reserves for alarm-clock apps,
 * so in deep sleep the reminder can come a few minutes late.
 */
class AlarmReminderAlarms(private val context: Context) : ReminderAlarms {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(r: MissionReminder) {
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, r.remindAt.toEpochMilli(), intent(r))
    }

    override fun cancel() = alarmManager.cancel(intent(null))

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
        Notifications.showMissionReminder(context, name, start)
        val store = (context.applicationContext as CbOnlineApp).container.reminderStore
        inBackground {
            // Clear only this reminder: a newer one may have replaced it meanwhile
            if (store.reminder.first()?.missionName == name) store.clear()
        }
    }
}

/**
 * Re-arms the pending reminder after the phone restarts or the app is updated, both of which drop alarms.
 * Exported for those system broadcasts, which other apps can't send.
 */
class RestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val container = (context.applicationContext as CbOnlineApp).container
        inBackground {
            val saved = container.reminderStore.reminder.first() ?: return@inBackground
            if (saved.start.isAfter(Instant.now())) container.reminderAlarms.schedule(saved)
            else container.reminderStore.clear()
        }
    }
}

/** Runs [work] off the main thread while keeping the receiver alive until it's done. */
private fun BroadcastReceiver.inBackground(work: suspend () -> Unit) {
    val pending = goAsync()
    CoroutineScope(Dispatchers.IO).launch {
        try {
            work()
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
