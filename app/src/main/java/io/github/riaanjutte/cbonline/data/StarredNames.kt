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

/** A set of starred names under one DataStore key, matched ignoring case and surrounding whitespace. */
internal class StarredNames(private val dataStore: DataStore<Preferences>, keyName: String) {

    private val key = stringSetPreferencesKey(keyName)

    val values: Flow<Set<String>> = dataStore.data
        .map { it[key] ?: emptySet() }
        .catch {
            if (it is IOException) {
                Log.w("CBOnline", "Reading ${key.name} failed", it)
                emit(emptySet())
            } else {
                throw it
            }
        }

    /** Adds the trimmed name, or removes the stored spelling that matches it. */
    suspend fun toggle(name: String) {
        val wanted = nameKey(name)
        dataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            val existing = current.firstOrNull { nameKey(it) == wanted }
            prefs[key] = if (existing != null) current - existing else current + name.trim()
        }
    }

    /** Adds the trimmed name unless a matching spelling is already stored. */
    suspend fun add(name: String) {
        val wanted = nameKey(name)
        dataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            if (current.none { nameKey(it) == wanted }) prefs[key] = current + name.trim()
        }
    }

    /** Removes the stored spelling that matches the name, if any. */
    suspend fun remove(name: String) {
        val wanted = nameKey(name)
        dataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = current.filterNot { nameKey(it) == wanted }.toSet()
        }
    }
}
