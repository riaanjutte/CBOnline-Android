package io.github.riaanjutte.cbonline.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.riaanjutte.cbonline.roster.isValidSquadTag
import kotlinx.coroutines.flow.Flow

interface SquadsRepository {
    val squads: Flow<Set<String>>
    suspend fun toggle(tag: String)
}

/** Starred squad tags ("=JG52="); every online pilot whose name contains one shows under Friends. */
class SquadsStore(dataStore: DataStore<Preferences>) : SquadsRepository {

    private val names = StarredNames(dataStore, "squads")

    override val squads: Flow<Set<String>> = names.values

    override suspend fun toggle(tag: String) {
        if (isValidSquadTag(tag)) names.toggle(tag)
    }
}
