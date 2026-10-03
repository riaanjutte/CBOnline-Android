package io.github.riaanjutte.cbonline.roster

import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.data.Coalition.Allied
import io.github.riaanjutte.cbonline.data.Coalition.Axis
import io.github.riaanjutte.cbonline.data.Coalition.Unassigned
import io.github.riaanjutte.cbonline.data.OnlinePlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RosterBuilderTest {

    private fun p(n: String, c: Coalition, t: String = "00:10") = OnlinePlayer(n, c, t)

    @Test
    fun `counts and sorts sides ignoring case`() {
        val r = RosterBuilder.build(
            listOf(p("charlie", Axis), p("Bravo", Allied), p("alpha", Allied), p("Zed", Unassigned)),
            emptySet()
        )
        assertEquals(listOf(4, 1, 2, 1), listOf(r.total, r.axisCount, r.alliedCount, r.unassignedCount))
        assertEquals(listOf("alpha", "Bravo"), r.allied.map { it.nickname })
        assertEquals(listOf("charlie"), r.axis.map { it.nickname })
        assertEquals(listOf("Zed"), r.unassigned.map { it.nickname })
        assertEquals(0, r.starredCount)
        assertTrue(r.friendsOnline.isEmpty() && r.friendsOffline.isEmpty())
    }

    @Test
    fun `friends split into online and offline`() {
        val r = RosterBuilder.build(
            listOf(p("Bob", Axis, "01:05"), p("carol", Allied)),
            setOf("bob", "Carol", "Dave")
        )
        assertEquals(listOf("Bob", "carol"), r.friendsOnline.map { it.nickname })
        assertEquals(listOf("Dave"), r.friendsOffline)
        assertEquals(3, r.starredCount)
        // friend stays in their side section
        assertEquals(RosterRow("Bob", Axis, "1 h 05 min", isFriend = true), r.axis.single())
        assertEquals(r.alliedCount, r.allied.size)
        assertTrue(r.allied.single().isFriend)
    }

    @Test
    fun `no unassigned players gives empty unassigned list`() {
        val r = RosterBuilder.build(listOf(p("A", Axis), p("B", Allied)), emptySet())
        assertTrue(r.unassigned.isEmpty())
        assertEquals(0, r.unassignedCount)
    }

    @Test
    fun `zero players`() {
        val r = RosterBuilder.build(emptyList(), setOf("Zoe", "adam"))
        assertEquals(0, r.total)
        assertTrue(r.axis.isEmpty() && r.allied.isEmpty() && r.unassigned.isEmpty() && r.friendsOnline.isEmpty())
        assertEquals(listOf("adam", "Zoe"), r.friendsOffline)
    }

    @Test
    fun `duplicate nicknames are kept`() {
        val r = RosterBuilder.build(listOf(p("Ghost", Axis), p("Ghost", Axis)), emptySet())
        assertEquals(2, r.axis.size)
        assertEquals(2, r.axisCount)
        assertEquals(2, r.total)
    }

    @Test
    fun `one starred name matching two players lists both`() {
        val r = RosterBuilder.build(listOf(p("Bob", Axis), p("BOB", Allied)), setOf("bob"))
        assertEquals(2, r.friendsOnline.size)
        assertTrue(r.friendsOffline.isEmpty())
    }

    @Test
    fun `online friend count counts stars, not rows`() {
        // one star matching two rows (reconnect ghost / case variant) is still one friend online
        val r = RosterBuilder.build(listOf(p("Bob", Axis), p("BOB", Allied), p("Carol", Axis)), setOf("bob", "Dave"))
        assertEquals(2, r.friendsOnline.size)
        assertEquals(1, r.friendsOnlineCount)
        assertEquals(2, r.starredCount)
    }

    @Test
    fun `symbol-heavy names sort predictably`() {
        val r = RosterBuilder.build(
            listOf(p("ace", Allied), p("Øystein", Allied), p("[CB]Ace", Allied), p("=JG52=Hans", Allied)),
            emptySet()
        )
        assertEquals(listOf("=JG52=Hans", "[CB]Ace", "ace", "Øystein"), r.allied.map { it.nickname })
    }
}
