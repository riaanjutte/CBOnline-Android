package io.github.riaanjutte.cbonline.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.riaanjutte.cbonline.notify.FriendAlertsRepository
import io.github.riaanjutte.cbonline.notify.MissionReminder
import io.github.riaanjutte.cbonline.notify.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Instant

/** The pending mission reminder, if any (mission name and its start time). */
class ReminderStore(private val dataStore: DataStore<Preferences>) : ReminderRepository {

    override val reminder: Flow<MissionReminder?> = dataStore.data
        .catchIo()
        .map { prefs ->
            val name = prefs[NAME] ?: return@map null
            val start = prefs[START] ?: return@map null
            MissionReminder(name, Instant.ofEpochMilli(start))
        }

    override suspend fun set(r: MissionReminder) {
        dataStore.edit {
            it[NAME] = r.missionName
            it[START] = r.start.toEpochMilli()
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(NAME)
            it.remove(START)
        }
    }

    private companion object {
        val NAME = stringPreferencesKey("reminder_mission")
        val START = longPreferencesKey("reminder_start")
    }
}

/** The friend alerts switch and who was online at the last background check (lower-case names). */
class FriendAlertsStore(private val dataStore: DataStore<Preferences>) : FriendAlertsRepository {

    private val data = dataStore.data.catchIo()

    override val enabled: Flow<Boolean> = data.map { it[ENABLED] ?: false }
    override val lastOnline: Flow<Set<String>> = data.map { it[LAST_ONLINE] ?: emptySet() }

    override suspend fun setEnabled(on: Boolean) {
        dataStore.edit { it[ENABLED] = on }
    }

    override suspend fun setLastOnline(keys: Set<String>) {
        dataStore.edit { it[LAST_ONLINE] = keys }
    }

    private companion object {
        val ENABLED = booleanPreferencesKey("friend_alerts")
        val LAST_ONLINE = stringSetPreferencesKey("friend_alerts_last_online")
    }
}

/** An unreadable settings file reads as empty rather than crashing, like the starred names. */
private fun Flow<Preferences>.catchIo(): Flow<Preferences> = catch {
    if (it is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw it
}
