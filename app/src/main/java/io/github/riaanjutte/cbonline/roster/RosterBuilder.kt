package io.github.riaanjutte.cbonline.roster

import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.data.OnlinePlayer
import java.util.Locale

data class RosterRow(
    val nickname: String,
    val coalition: Coalition,
    val timeLabel: String,
    val isFriend: Boolean
)

/** Everything the roster screen shows, already counted, split and sorted. */
data class Roster(
    val total: Int,
    val axisCount: Int,
    val alliedCount: Int,
    val unassignedCount: Int,
    val friendsOnline: List<RosterRow>,
    /** Starred display names with no matching online player. */
    val friendsOffline: List<String>,
    val starredCount: Int,
    /** Starred names with at least one online match; one star can match several rows. */
    val friendsOnlineCount: Int,
    val axis: List<RosterRow>,
    val allied: List<RosterRow>,
    val unassigned: List<RosterRow>
)

object RosterBuilder {

    fun build(players: List<OnlinePlayer>, friends: Set<String>): Roster {
        val friendKeys = friends.map(::friendKey).toSet()
        // sortedWith is stable, so duplicate nicknames keep API order
        val rows = players
            .map { RosterRow(it.nickname, it.coalition, formatTimeOnMission(it.timeOnMission), friendKey(it.nickname) in friendKeys) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nickname })
        val onlineKeys = rows.map { friendKey(it.nickname) }.toSet()
        val axis = rows.filter { it.coalition == Coalition.Axis }
        val allied = rows.filter { it.coalition == Coalition.Allied }
        val unassigned = rows.filter { it.coalition == Coalition.Unassigned }
        val friendsOffline = friends.filter { friendKey(it) !in onlineKeys }.sortedWith(String.CASE_INSENSITIVE_ORDER)
        return Roster(
            total = rows.size,
            axisCount = axis.size,
            alliedCount = allied.size,
            unassignedCount = unassigned.size,
            friendsOnline = rows.filter { it.isFriend },
            friendsOffline = friendsOffline,
            starredCount = friends.size,
            friendsOnlineCount = friends.size - friendsOffline.size,
            axis = axis,
            allied = allied,
            unassigned = unassigned
        )
    }

    private fun friendKey(name: String) = name.trim().lowercase(Locale.ROOT)
}
