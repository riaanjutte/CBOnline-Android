package io.github.riaanjutte.cbonline

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.riaanjutte.cbonline.data.FriendAlertsStore
import io.github.riaanjutte.cbonline.data.FriendsStore
import io.github.riaanjutte.cbonline.data.ReminderStore
import io.github.riaanjutte.cbonline.notify.AlarmReminderAlarms
import io.github.riaanjutte.cbonline.notify.FriendAlertCheck
import io.github.riaanjutte.cbonline.notify.FriendAlertSwitch
import io.github.riaanjutte.cbonline.notify.MissionReminders
import io.github.riaanjutte.cbonline.notify.Notifications
import io.github.riaanjutte.cbonline.notify.WorkManagerAlertScheduler
import io.github.riaanjutte.cbonline.data.MissionInfoApi
import io.github.riaanjutte.cbonline.data.OnlinePlayersApi
import io.github.riaanjutte.cbonline.data.PilotStatsApi
import io.github.riaanjutte.cbonline.data.SquadsStore
import io.github.riaanjutte.cbonline.data.UpdateChecker
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** App-wide singletons, created once in [CbOnlineApp]. */
class AppContainer(context: Context) {

    private val client = OkHttpClient.Builder().callTimeout(10, TimeUnit.SECONDS).build()
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    // One DataStore per file: friends and the dismissed update version share it
    private val dataStore = PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }
    private val userAgent = "CBOnline-Android/${BuildConfig.VERSION_NAME}"

    val playersApi = OnlinePlayersApi(
        client, json, BuildConfig.API_BASE_URL, userAgent,
        log = { if (BuildConfig.DEBUG) Log.d("CBOnline", it) }
    )
    val missionApi = MissionInfoApi(
        client, json, BuildConfig.MISSION_URL, userAgent,
        log = { if (BuildConfig.DEBUG) Log.d("CBOnline", it) }
    )
    val statsApi = PilotStatsApi(
        client, json, BuildConfig.API_BASE_URL, userAgent,
        log = { if (BuildConfig.DEBUG) Log.d("CBOnline", it) }
    )
    val friendsStore = FriendsStore(dataStore)
    val squadsStore = SquadsStore(dataStore)

    val reminderStore = ReminderStore(dataStore)
    val reminderAlarms = AlarmReminderAlarms(context.applicationContext)
    val missionReminders = MissionReminders(reminderStore, reminderAlarms)

    private val friendAlertsStore = FriendAlertsStore(dataStore)
    val friendAlertSwitch = FriendAlertSwitch(friendAlertsStore, WorkManagerAlertScheduler(context.applicationContext))
    private val appContext = context.applicationContext

    /** One background friend check; [inForeground] keeps it quiet while the app is on screen. */
    fun friendAlertCheck(inForeground: () -> Boolean) = FriendAlertCheck(
        playersApi, friendsStore, squadsStore, friendAlertsStore,
        notify = { Notifications.showFriendsOnline(appContext, it) },
        inForeground = inForeground
    )
    val updateChecker = UpdateChecker(client, json, dataStore, userAgent)
}
