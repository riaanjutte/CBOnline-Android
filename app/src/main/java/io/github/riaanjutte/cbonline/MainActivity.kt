package io.github.riaanjutte.cbonline

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.riaanjutte.cbonline.notify.AlarmReminderAlarms
import io.github.riaanjutte.cbonline.notify.Notifications
import io.github.riaanjutte.cbonline.ui.PilotStatsSheet
import io.github.riaanjutte.cbonline.ui.PilotStatsViewModel
import io.github.riaanjutte.cbonline.ui.RosterScreen
import io.github.riaanjutte.cbonline.ui.RosterViewModel
import io.github.riaanjutte.cbonline.ui.StatsUiState
import io.github.riaanjutte.cbonline.ui.theme.CbOnlineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always dark, so system bar icons must be light even when the phone is in light mode
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        val container = (application as CbOnlineApp).container
        setContent {
            CbOnlineTheme {
                val vm: RosterViewModel = viewModel(
                    factory = viewModelFactory {
                        initializer {
                            RosterViewModel(
                                container.playersApi,
                                container.missionApi,
                                container.friendsStore,
                                container.squadsStore,
                                container.missionReminders,
                                container.friendAlertSwitch,
                                container.updateChecker,
                                BuildConfig.VERSION_NAME
                            )
                        }
                    }
                )
                val statsVm: PilotStatsViewModel = viewModel(
                    factory = viewModelFactory { initializer { PilotStatsViewModel(container.statsApi) } }
                )
                val state by vm.state.collectAsStateWithLifecycle()
                val stats by statsVm.state.collectAsStateWithLifecycle()
                val reminderSet = state.reminder != null && state.reminder?.missionName == state.mission?.next?.name
                val context = LocalContext.current
                val turnOn = rememberNotificationGate { feature ->
                    when (feature) {
                        NotifyFeature.Reminder -> {
                            vm.toggleReminder()
                            askOnceForExactAlarms(context)
                        }
                        NotifyFeature.FriendAlerts -> vm.setFriendAlerts(true)
                    }
                }
                RosterScreen(
                    state = state,
                    versionName = BuildConfig.VERSION_NAME,
                    onRefresh = vm::refresh,
                    onToggleFriend = vm::toggleFriend,
                    onUnstarSquad = vm::unstarSquad,
                    onOpenStats = statsVm::open,
                    // Switching off never needs permission; switching on asks for it first
                    onToggleReminder = { if (reminderSet) vm.toggleReminder() else turnOn(NotifyFeature.Reminder) },
                    onToggleFriendAlerts = { if (state.friendAlertsOn) vm.setFriendAlerts(false) else turnOn(NotifyFeature.FriendAlerts) },
                    onDismissUpdate = vm::dismissUpdate,
                    // No browser installed is the only failure; nothing useful to show for it
                    onOpenUrl = { runCatching { startActivity(Intent(Intent.ACTION_VIEW, it.toUri())) } }
                )
                if (stats != StatsUiState.Hidden) {
                    PilotStatsSheet(
                        stats, state.squads,
                        onRetry = statsVm::retry, onStarSquad = vm::starSquad, onUnstarSquad = vm::unstarSquad, onClose = statsVm::close
                    )
                }
            }
        }
    }
}

/** What's being switched on while Android's notification prompt is open. */
private enum class NotifyFeature { Reminder, FriendAlerts }

/**
 * Returns a function that switches a feature on once notifications are allowed: straight away if they are,
 * after Android's permission prompt if not (Android 13+), and otherwise explains where to turn them on. The
 * waiting feature is saved, so a rotation while the prompt is open doesn't lose it.
 */
@Composable
private fun rememberNotificationGate(onAllowed: (NotifyFeature) -> Unit): (NotifyFeature) -> Unit {
    val context = LocalContext.current
    var pending by rememberSaveable { mutableStateOf<NotifyFeature?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val feature = pending
        pending = null
        if (granted && Notifications.canNotify(context)) feature?.let(onAllowed) else showNotificationsOff(context)
    }
    return { feature ->
        when {
            Notifications.canNotify(context) -> onAllowed(feature)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                pending = feature
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> showNotificationsOff(context)
        }
    }
}

private fun showNotificationsOff(context: Context) =
    Toast.makeText(context, R.string.notifications_off, Toast.LENGTH_LONG).show()

/**
 * The first time a reminder is set on a phone that needs it (Android 12+), opens Android's "Alarms & reminders"
 * switch for the app, so reminders come on time. Only once: left off, reminders still come, just less precisely.
 */
private fun askOnceForExactAlarms(context: Context) {
    if (!AlarmReminderAlarms.needsExactAlarmAccess(context) || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val prefs = context.getSharedPreferences("ui", Context.MODE_PRIVATE)
    if (prefs.getBoolean("exact_alarms_asked", false)) return
    prefs.edit().putBoolean("exact_alarms_asked", true).apply()
    Toast.makeText(context, R.string.exact_alarms_prompt, Toast.LENGTH_LONG).show()
    runCatching {
        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri()))
    }
}
