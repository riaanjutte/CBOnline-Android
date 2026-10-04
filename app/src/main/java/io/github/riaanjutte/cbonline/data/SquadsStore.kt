package io.github.riaanjutte.cbonline.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.riaanjutte.cbonline.roster.isValidSquadTag
import kotlinx.coroutines.flow.Flow

/** Separate add and remove, never toggle: the buttons say which they do, so repeating one mustn't undo it. */
interface SquadsRepository {
    val squads: Flow<Set<String>>
    suspend fun add(tag: String)
    suspend fun remove(tag: String)
}

/** Starred squad tags ("=JG52="); every online pilot whose name contains one shows under Friends. */
class SquadsStore(dataStore: DataStore<Preferences>) : SquadsRepository {

    private val names = StarredNames(dataStore, "squads")

    override val squads: Flow<Set<String>> = names.values

    override suspend fun add(tag: String) {
        if (isValidSquadTag(tag)) names.add(tag)
    }

    override suspend fun remove(tag: String) = names.remove(tag)
}
