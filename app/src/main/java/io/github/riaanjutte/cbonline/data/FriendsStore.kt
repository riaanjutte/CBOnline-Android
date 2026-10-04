package io.github.riaanjutte.cbonline.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

interface FriendsRepository {
    val friends: Flow<Set<String>>
    suspend fun toggle(nickname: String)
}

/** Starred nicknames, matched ignoring case and surrounding whitespace. */
class FriendsStore(dataStore: DataStore<Preferences>) : FriendsRepository {

    private val names = StarredNames(dataStore, "friends")

    override val friends: Flow<Set<String>> = names.values

    override suspend fun toggle(nickname: String) = names.toggle(nickname)
}
