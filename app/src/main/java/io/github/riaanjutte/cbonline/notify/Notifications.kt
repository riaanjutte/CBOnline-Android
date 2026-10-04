package io.github.riaanjutte.cbonline.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.riaanjutte.cbonline.MainActivity
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.roster.RosterRow
import java.time.Instant
import java.util.Date

/** The app's two kinds of notification, each in its own channel so either can be muted in Android settings. */
object Notifications {

    private const val CHANNEL_REMINDERS = "mission_reminders"
    private const val CHANNEL_FRIENDS = "friend_alerts"
    private const val ID_REMINDER = 1
    private const val ID_FRIENDS = 2

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_REMINDERS, context.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = context.getString(R.string.channel_reminders_text) },
                NotificationChannel(CHANNEL_FRIENDS, context.getString(R.string.channel_friends), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = context.getString(R.string.channel_friends_text) }
            )
        )
    }

    /** Android 13+ asks per app; on any version the user can also switch the app's notifications off. */
    fun canNotify(context: Context): Boolean {
        val permitted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return permitted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showMissionReminder(context: Context, missionName: String, start: Instant) {
        val time = DateFormat.getTimeFormat(context).format(Date.from(start))
        show(context, ID_REMINDER, CHANNEL_REMINDERS, context.getString(R.string.notif_reminder_title),
            context.getString(R.string.notif_reminder_text, missionName, time))
    }

    fun showFriendsOnline(context: Context, rows: List<RosterRow>) {
        val summary = summarise(rows.map { it.nickname })
        val names = summary.shown
        val text = when {
            summary.others > 0 -> context.resources.getQuantityString(R.plurals.notif_friends_more, summary.others, names[0], names[1], summary.others)
            names.size == 1 -> context.getString(R.string.notif_friends_one, names[0])
            names.size == 2 -> context.getString(R.string.notif_friends_two, names[0], names[1])
            else -> context.getString(R.string.notif_friends_three, names[0], names[1], names[2])
        }
        show(context, ID_FRIENDS, CHANNEL_FRIENDS, context.getString(R.string.notif_friends_title), text)
    }

    private fun show(context: Context, id: Int, channel: String, title: String, text: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (e: SecurityException) {
            Log.w("CBOnline", "Notification permission withdrawn", e) // revoked between the check and now
        }
    }
}
