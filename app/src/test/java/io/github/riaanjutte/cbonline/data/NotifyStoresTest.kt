package io.github.riaanjutte.cbonline.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.riaanjutte.cbonline.notify.MissionReminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Instant

class NotifyStoresTest {

    @get:Rule
    val tmp = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val dataStore: DataStore<Preferences> by lazy {
        PreferenceDataStoreFactory.create(scope = scope) { File(tmp.root, "settings.preferences_pb") }
    }

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `reminder is saved and cleared`() = runTest {
        val store = ReminderStore(dataStore)
        assertNull(store.reminder.first())
        val r = MissionReminder("Operation Paravane", Instant.parse("2026-10-04T07:43:00Z"))
        store.set(r)
        assertEquals(r, store.reminder.first())
        store.clear()
        assertNull(store.reminder.first())
    }

    @Test
    fun `friend alerts start off, then keep their switch and last-online list`() = runTest {
        val store = FriendAlertsStore(dataStore)
        assertEquals(false, store.enabled.first())
        assertEquals(emptySet<String>(), store.lastOnline.first())
        store.setEnabled(true)
        store.setLastOnline(setOf("hans", "=jg52=otto"))
        assertEquals(true, store.enabled.first())
        assertEquals(setOf("hans", "=jg52=otto"), store.lastOnline.first())
    }
}
