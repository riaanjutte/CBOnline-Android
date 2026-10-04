package io.github.riaanjutte.cbonline.notify

import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.data.FriendsRepository
import io.github.riaanjutte.cbonline.data.OnlinePlayer
import io.github.riaanjutte.cbonline.data.PlayersSource
import io.github.riaanjutte.cbonline.data.SquadsRepository
import io.github.riaanjutte.cbonline.roster.RosterRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class FriendAlertsTest {

    private fun row(n: String) = RosterRow(n, Coalition.Axis, "10 min", isFriend = true)

    @Test
    fun `newly online ignores case and duplicates`() {
        assertEquals(
            listOf("Otto"),
            newlyOnline(setOf("hans"), listOf(row("Hans"), row("Otto"), row("otto"))).map { it.nickname }
        )
    }

    @Test
    fun `summaries name up to three pilots, then two and a count`() {
        assertEquals(AlertSummary(listOf("A"), 0), summarise(listOf("A")))
        assertEquals(AlertSummary(listOf("A", "B", "C"), 0), summarise(listOf("A", "B", "C")))
        assertEquals(AlertSummary(listOf("A", "B"), 3), summarise(listOf("A", "B", "C", "D", "E")))
    }

    // The background check, with everything it talks to faked

    private class FakePlayers(var players: List<OnlinePlayer>, var fail: Boolean = false) : PlayersSource {
        var fetches = 0
        override suspend fun fetch(): List<OnlinePlayer> {
            fetches++
            return if (fail) throw IOException("offline") else players
        }
    }

    private class FakeFriends(names: Set<String>) : FriendsRepository {
        override val friends = MutableStateFlow(names)
        override suspend fun toggle(nickname: String) = Unit
    }

    private class FakeSquads(tags: Set<String>) : SquadsRepository {
        override val squads = MutableStateFlow(tags)
        override suspend fun add(tag: String) = Unit
        override suspend fun remove(tag: String) = Unit
    }

    private class FakeAlertsStore(enabled: Boolean, last: Set<String>?) : FriendAlertsRepository {
        override val enabled = MutableStateFlow(enabled)
        override val lastOnline = MutableStateFlow(last)
        override suspend fun setEnabled(on: Boolean) { enabled.value = on }
        override suspend fun setLastOnline(keys: Set<String>?) { lastOnline.value = keys }
    }

    private fun p(n: String) = OnlinePlayer(n, Coalition.Allied, "00:10")

    private val notified = mutableListOf<List<String>>()
    private fun check(
        players: FakePlayers,
        store: FakeAlertsStore,
        foreground: Boolean = false,
        canNotify: Boolean = true,
        friends: Set<String> = setOf("Hans"),
        squads: Set<String> = setOf("=JG52=")
    ) = FriendAlertCheck(
        players, FakeFriends(friends), FakeSquads(squads), store,
        notify = { rows -> notified += rows.map { it.nickname } },
        inForeground = { foreground },
        canNotify = { canNotify }
    )

    @Test
    fun `with no list yet, the first check only records who's online`() = runTest {
        val store = FakeAlertsStore(enabled = true, last = null)
        check(FakePlayers(listOf(p("Hans"))), store).run()
        assertTrue(notified.isEmpty())
        assertEquals(setOf("hans"), store.lastOnline.value)
    }

    @Test
    fun `with notifications blocked there's no check at all`() = runTest {
        val players = FakePlayers(listOf(p("Hans")))
        val store = FakeAlertsStore(enabled = true, last = emptySet())
        check(players, store, canNotify = false).run()
        assertEquals(0, players.fetches)
        assertEquals(emptySet<String>(), store.lastOnline.value)
    }

    @Test
    fun `with nothing starred there's no request`() = runTest {
        val players = FakePlayers(listOf(p("Hans")))
        check(players, FakeAlertsStore(enabled = true, last = emptySet()), friends = emptySet(), squads = emptySet()).run()
        assertEquals(0, players.fetches)
    }

    @Test
    fun `alerts for friends and squad members who came online since the last check`() = runTest {
        val store = FakeAlertsStore(enabled = true, last = setOf("hans"))
        check(FakePlayers(listOf(p("Hans"), p("=JG52=Otto"), p("Bob"))), store).run()
        assertEquals(listOf(listOf("=JG52=Otto")), notified)
        assertEquals(setOf("hans", "=jg52=otto"), store.lastOnline.value)
    }

    @Test
    fun `nothing new, no alert`() = runTest {
        val store = FakeAlertsStore(enabled = true, last = setOf("hans"))
        check(FakePlayers(listOf(p("Hans"))), store).run()
        assertTrue(notified.isEmpty())
    }

    @Test
    fun `a friend who went offline alerts again when they come back`() = runTest {
        val store = FakeAlertsStore(enabled = true, last = setOf("hans"))
        val players = FakePlayers(listOf(p("Bob")))
        check(players, store).run()
        assertEquals(emptySet<String>(), store.lastOnline.value)
        players.players = listOf(p("Hans"), p("Bob"))
        check(players, store).run()
        assertEquals(listOf(listOf("Hans")), notified)
    }

    @Test
    fun `an empty server keeps the last list, so friends aren't alerted again when it fills`() = runTest {
        val store = FakeAlertsStore(enabled = true, last = setOf("hans"))
        val players = FakePlayers(emptyList())
        check(players, store).run()
        assertEquals(setOf("hans"), store.lastOnline.value)
        players.players = listOf(p("Hans"))
        check(players, store).run()
        assertTrue(notified.isEmpty())
    }

    @Test
    fun `with the app open there's no alert, but the check still remembers who's online`() = runTest {
        val store = FakeAlertsStore(enabled = true, last = emptySet())
        check(FakePlayers(listOf(p("Hans"))), store, foreground = true).run()
        assertTrue(notified.isEmpty())
        assertEquals(setOf("hans"), store.lastOnline.value)
    }

    @Test
    fun `switched off, it does nothing`() = runTest {
        val store = FakeAlertsStore(enabled = false, last = emptySet())
        check(FakePlayers(listOf(p("Hans"))), store).run()
        assertTrue(notified.isEmpty())
        assertEquals(emptySet<String>(), store.lastOnline.value)
    }

    @Test
    fun `a failed fetch changes nothing`() = runTest {
        val store = FakeAlertsStore(enabled = true, last = setOf("hans"))
        check(FakePlayers(emptyList(), fail = true), store).run()
        assertTrue(notified.isEmpty())
        assertEquals(setOf("hans"), store.lastOnline.value)
    }
}
