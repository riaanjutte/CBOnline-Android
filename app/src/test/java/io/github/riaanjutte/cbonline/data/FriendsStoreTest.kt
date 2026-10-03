package io.github.riaanjutte.cbonline.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FriendsStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scopes = mutableListOf<CoroutineScope>()

    private fun newScope() = CoroutineScope(Dispatchers.IO + Job()).also { scopes += it }

    private fun newStore(scope: CoroutineScope) = FriendsStore(
        PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") }
    )

    @After
    fun tearDown() = scopes.forEach { it.cancel() }

    @Test
    fun `toggle adds then removes`() = runTest {
        val store = newStore(newScope())
        store.toggle("Bob")
        assertEquals(setOf("Bob"), store.friends.first())
        store.toggle("Bob")
        assertEquals(emptySet<String>(), store.friends.first())
    }

    @Test
    fun `toggle off ignores case and whitespace`() = runTest {
        val store = newStore(newScope())
        store.toggle("Bob")
        store.toggle("  bob ")
        assertEquals(emptySet<String>(), store.friends.first())
    }

    @Test
    fun `stores trimmed first-starred name`() = runTest {
        val store = newStore(newScope())
        store.toggle(" Bob ")
        assertEquals(setOf("Bob"), store.friends.first())
    }

    @Test
    fun `persists across store instances`() = runTest {
        val scope1 = newScope()
        newStore(scope1).toggle("Bob")
        // DataStore releases the file only once its scope's job has completed
        scope1.coroutineContext.job.cancelAndJoin()
        assertEquals(setOf("Bob"), newStore(newScope()).friends.first())
    }
}
