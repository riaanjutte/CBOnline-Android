package io.github.riaanjutte.cbonline.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SquadsStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") }
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `toggle adds a trimmed tag then removes it ignoring case`() = runTest {
        val store = SquadsStore(dataStore)
        store.toggle(" =JG52= ")
        assertEquals(setOf("=JG52="), store.squads.first())
        store.toggle("=jg52=")
        assertEquals(emptySet<String>(), store.squads.first())
    }

    @Test
    fun `tags shorter than two characters are ignored`() = runTest {
        val store = SquadsStore(dataStore)
        store.toggle("x")
        store.toggle("   ")
        assertEquals(emptySet<String>(), store.squads.first())
    }

    @Test
    fun `squads and friends are kept apart in the same file`() = runTest {
        val squads = SquadsStore(dataStore)
        val friends = FriendsStore(dataStore)
        squads.toggle("[CB]")
        friends.toggle("Bob")
        assertEquals(setOf("[CB]"), squads.squads.first())
        assertEquals(setOf("Bob"), friends.friends.first())
    }
}
