package io.github.riaanjutte.cbonline

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.riaanjutte.cbonline.data.FriendsStore
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
    val updateChecker = UpdateChecker(client, json, dataStore, userAgent)
}
