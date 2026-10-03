package io.github.riaanjutte.cbonline.data

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Locale

interface FriendsRepository {
    val friends: Flow<Set<String>>
    suspend fun toggle(nickname: String)
}

/** Starred nicknames, matched ignoring case and surrounding whitespace. */
class FriendsStore(private val dataStore: DataStore<Preferences>) : FriendsRepository {

    override val friends: Flow<Set<String>> = dataStore.data
        .map { it[FRIENDS] ?: emptySet() }
        .catch {
            if (it is IOException) {
                Log.w("CBOnline", "Reading friends failed", it)
                emit(emptySet())
            } else {
                throw it
            }
        }

    override suspend fun toggle(nickname: String) {
        val key = normalise(nickname)
        dataStore.edit { prefs ->
            val current = prefs[FRIENDS] ?: emptySet()
            val existing = current.firstOrNull { normalise(it) == key }
            prefs[FRIENDS] = if (existing != null) current - existing else current + nickname.trim()
        }
    }

    private fun normalise(name: String) = name.trim().lowercase(Locale.ROOT)

    private companion object {
        val FRIENDS = stringSetPreferencesKey("friends")
    }
}
