package io.github.riaanjutte.cbonline.roster

import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.data.OnlinePlayer
import io.github.riaanjutte.cbonline.data.nameKey

data class RosterRow(
    val nickname: String,
    val coalition: Coalition,
    val timeLabel: String,
    /** Starred individually. */
    val isFriend: Boolean,
    /** The starred squad tag this pilot's name contains, if any. */
    val squad: String? = null
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
    val unassigned: List<RosterRow>,
    /** Starred squad tags with no online member. */
    val squadsOffline: List<String> = emptyList()
)

object RosterBuilder {

    /**
     * Friends online are pilots starred individually or by squad, each listed once. The starred-pilot counts
     * ([Roster.starredCount], [Roster.friendsOnlineCount]) leave squads out.
     */
    fun build(players: List<OnlinePlayer>, friends: Set<String>, squads: Set<String> = emptySet()): Roster {
        val nameKeys = friends.map(::nameKey).toSet()
        val squadTags = squads.sortedWith(String.CASE_INSENSITIVE_ORDER)
        // sortedWith is stable, so duplicate nicknames keep API order
        val rows = players
            .map {
                RosterRow(
                    it.nickname, it.coalition, formatTimeOnMission(it.timeOnMission),
                    isFriend = nameKey(it.nickname) in nameKeys,
                    squad = squadTags.firstOrNull { tag -> isSquadMember(it.nickname, tag) }
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.nickname })
        val onlineKeys = rows.map { nameKey(it.nickname) }.toSet()
        val axis = rows.filter { it.coalition == Coalition.Axis }
        val allied = rows.filter { it.coalition == Coalition.Allied }
        val unassigned = rows.filter { it.coalition == Coalition.Unassigned }
        val friendsOffline = friends.filter { nameKey(it) !in onlineKeys }.sortedWith(String.CASE_INSENSITIVE_ORDER)
        return Roster(
            total = rows.size,
            axisCount = axis.size,
            alliedCount = allied.size,
            unassignedCount = unassigned.size,
            friendsOnline = rows.filter { it.isFriend || it.squad != null },
            friendsOffline = friendsOffline,
            starredCount = friends.size,
            friendsOnlineCount = friends.size - friendsOffline.size,
            axis = axis,
            allied = allied,
            unassigned = unassigned,
            squadsOffline = squadTags.filter { tag -> rows.none { isSquadMember(it.nickname, tag) } }
        )
    }
}
