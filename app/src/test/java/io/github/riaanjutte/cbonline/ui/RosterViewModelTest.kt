package io.github.riaanjutte.cbonline.ui

import io.github.riaanjutte.cbonline.MainDispatcherRule
import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.data.FriendsRepository
import io.github.riaanjutte.cbonline.data.OnlinePlayer
import io.github.riaanjutte.cbonline.data.PlayersSource
import io.github.riaanjutte.cbonline.data.UpdateInfo
import io.github.riaanjutte.cbonline.data.UpdateSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RosterViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class FakePlayers : PlayersSource {
        var next: () -> List<OnlinePlayer> = { TWO_PLAYERS }
        var calls = 0
        var gate: CompletableDeferred<Unit>? = null
        override suspend fun fetch(): List<OnlinePlayer> {
            calls++
            gate?.await()
            return next()
        }
    }

    private class FakeFriends : FriendsRepository {
        override val friends = MutableStateFlow(emptySet<String>())
        var failWrites = false
        override suspend fun toggle(nickname: String) {
            if (failWrites) throw IOException("disk full")
            friends.value = if (nickname in friends.value) friends.value - nickname else friends.value + nickname
        }
    }

    private class FakeUpdates(private val info: UpdateInfo? = null) : UpdateSource {
        var checks = 0
        var dismissed: String? = null
        var failDismiss = false
        override suspend fun check(currentVersion: String): UpdateInfo? {
            checks++
            return info
        }
        override suspend fun dismiss(version: String) {
            if (failDismiss) throw IOException("disk full")
            dismissed = version
        }
    }

    private val players = FakePlayers()
    private val friends = FakeFriends()

    private fun vm(updates: UpdateSource = FakeUpdates()) = RosterViewModel(players, friends, updates, "1.0.0")

    private fun TestScope.subscribe(vm: RosterViewModel) =
        backgroundScope.launch { vm.state.collect {} }.also { runCurrent() }

    @Test
    fun `first load success shows roster`() = runTest {
        val vm = vm()
        subscribe(vm)
        val s = vm.state.value
        assertEquals(2, s.roster!!.total)
        assertFalse(s.isLoading)
        assertNotNull(s.lastUpdated)
        assertEquals(1, players.calls)
    }

    @Test
    fun `first load failure shows error`() = runTest {
        players.next = { throw IOException("HTTP 503") }
        val vm = vm()
        subscribe(vm)
        val s = vm.state.value
        assertNull(s.roster)
        assertEquals("HTTP 503", s.errorMessage)
        assertFalse(s.isLoading)
    }

    @Test
    fun `failed refresh keeps data until next success`() = runTest {
        val vm = vm()
        subscribe(vm)
        players.next = { throw IOException("HTTP 503") }
        advanceTimeBy(60_001)
        assertEquals(2, vm.state.value.roster!!.total)
        assertTrue(vm.state.value.refreshFailed)
        assertNull(vm.state.value.errorMessage)

        players.next = { TWO_PLAYERS }
        advanceTimeBy(60_001)
        assertFalse(vm.state.value.refreshFailed)
    }

    @Test
    fun `polls every 60 seconds while subscribed`() = runTest {
        subscribe(vm())
        advanceTimeBy(180_001)
        assertEquals(4, players.calls)
    }

    @Test
    fun `stops polling when unsubscribed and fetches on return`() = runTest {
        val vm = vm()
        val sub = subscribe(vm)
        sub.cancel()
        advanceTimeBy(5_000 + 180_000)
        assertEquals(1, players.calls)

        subscribe(vm)
        assertEquals(2, players.calls)
    }

    @Test
    fun `manual refresh during in-flight fetch is ignored`() = runTest {
        val gate = CompletableDeferred<Unit>()
        players.gate = gate
        val vm = vm()
        subscribe(vm)
        vm.refresh()
        vm.refresh()
        gate.complete(Unit)
        runCurrent()
        assertEquals(1, players.calls)
    }

    @Test
    fun `manual refresh is immediate, flags refreshing, restarts timer`() = runTest {
        val vm = vm()
        subscribe(vm)
        advanceTimeBy(30_000) // refresh mid-interval so a restarted timer is distinguishable
        val gate = CompletableDeferred<Unit>()
        players.gate = gate
        vm.refresh()
        runCurrent()
        assertEquals(2, players.calls)
        assertTrue(vm.state.value.isRefreshing)

        gate.complete(Unit)
        runCurrent()
        assertFalse(vm.state.value.isRefreshing)
        advanceTimeBy(59_000) // t = 89s: the original 60s tick must not fire
        assertEquals(2, players.calls)
        advanceTimeBy(1_001) // t = 90s: 60s after the manual refresh
        assertEquals(3, players.calls)
    }

    @Test
    fun `auto poll does not flag refreshing`() = runTest {
        val vm = vm()
        subscribe(vm)
        players.gate = CompletableDeferred()
        advanceTimeBy(60_001)
        assertEquals(2, players.calls)
        assertFalse(vm.state.value.isRefreshing)
    }

    @Test
    fun `toggling a friend does not fetch`() = runTest {
        players.next = { listOf(OnlinePlayer("Bob", Coalition.Axis, "00:10")) }
        val vm = vm()
        subscribe(vm)
        vm.toggleFriend("Bob")
        runCurrent()
        assertEquals(listOf("Bob"), vm.state.value.roster!!.friendsOnline.map { it.nickname })
        assertEquals(1, players.calls)
    }

    @Test
    fun `update shown once after first success and dismissible`() = runTest {
        val updates = FakeUpdates(UpdateInfo("1.1.0", "u"))
        val vm = vm(updates)
        subscribe(vm)
        assertEquals(UpdateInfo("1.1.0", "u"), vm.state.value.update)
        advanceTimeBy(120_001)
        assertEquals(1, updates.checks)

        vm.dismissUpdate()
        runCurrent()
        assertNull(vm.state.value.update)
        assertEquals("1.1.0", updates.dismissed)
    }

    @Test
    fun `failed friend write does not crash`() = runTest {
        val vm = vm()
        subscribe(vm)
        friends.failWrites = true
        vm.toggleFriend("Alpha")
        runCurrent()
        assertTrue(vm.state.value.roster!!.friendsOnline.isEmpty())
    }

    @Test
    fun `failed dismiss write does not crash and still hides the banner`() = runTest {
        val updates = FakeUpdates(UpdateInfo("1.1.0", "u")).apply { failDismiss = true }
        val vm = vm(updates)
        subscribe(vm)
        vm.dismissUpdate()
        runCurrent()
        assertNull(vm.state.value.update)
    }

    @Test
    fun `no update check after failed load`() = runTest {
        players.next = { throw IOException("HTTP 503") }
        val updates = FakeUpdates(UpdateInfo("1.1.0", "u"))
        subscribe(vm(updates))
        assertEquals(0, updates.checks)
    }

    private companion object {
        val TWO_PLAYERS = listOf(
            OnlinePlayer("Alpha", Coalition.Allied, "00:12"),
            OnlinePlayer("Bravo", Coalition.Axis, "01:05")
        )
    }
}
