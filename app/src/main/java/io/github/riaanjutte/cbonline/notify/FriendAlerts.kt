package io.github.riaanjutte.cbonline.notify

import android.util.Log
import io.github.riaanjutte.cbonline.data.FriendsRepository
import io.github.riaanjutte.cbonline.data.MissionInfo
import io.github.riaanjutte.cbonline.data.PlayersSource
import io.github.riaanjutte.cbonline.data.SquadsRepository
import io.github.riaanjutte.cbonline.data.nameKey
import io.github.riaanjutte.cbonline.roster.Roster
import io.github.riaanjutte.cbonline.roster.RosterBuilder
import io.github.riaanjutte.cbonline.roster.RosterRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

/** Friends online now who weren't at the last check, each once. */
fun newlyOnline(previous: Set<String>, nowOnline: List<RosterRow>): List<RosterRow> =
    nowOnline.filter { nameKey(it.nickname) !in previous }.distinctBy { nameKey(it.nickname) }

/**
 * From 2 minutes before to 5 minutes after a mission change: the shown mission's start, or its estimated end until
 * the feed moves on. The server lists nobody for a minute or two around then, so an empty roster says nothing.
 */
fun nearMissionChange(mission: MissionInfo?, now: Instant): Boolean =
    mission != null && listOfNotNull(mission.startedAt, mission.estimatedEnd).any { change ->
        !now.isBefore(change.minus(CHANGE_LEAD)) && !now.isAfter(change.plus(CHANGE_TAIL))
    }

/**
 * Who counts as already online when friend alerts are switched on: the friends in [roster] (lower-case names). Null
 * leaves the first background check to record who's on: when no roster has loaded, or when the roster is the empty
 * list the server shows at a mission change (judged by when it was fetched, [fetchedAt], since the screen can keep
 * showing it for a while). A quiet server's empty list is kept, so the first friend to come on alerts.
 */
fun onlineAtSwitchOn(roster: Roster?, mission: MissionInfo?, fetchedAt: Instant): Set<String>? {
    if (roster == null || (roster.total == 0 && nearMissionChange(mission, fetchedAt))) return null
    return roster.friendsOnline.map { nameKey(it.nickname) }.toSet()
}

private val CHANGE_LEAD = Duration.ofMinutes(2)
private val CHANGE_TAIL = Duration.ofMinutes(5)

/** Names for a notification: all of them up to three, otherwise two plus how many more. */
data class AlertSummary(val shown: List<String>, val others: Int)

fun summarise(names: List<String>): AlertSummary =
    if (names.size <= 3) AlertSummary(names, 0) else AlertSummary(names.take(2), names.size - 2)

interface FriendAlertsRepository {
    val enabled: Flow<Boolean>
    /** Who was online at the last check (lower-case names); null when there's no list yet. */
    val lastOnline: Flow<Set<String>?>
    suspend fun setEnabled(on: Boolean)
    suspend fun setLastOnline(keys: Set<String>?)
}

/** Android's periodic background check. */
interface AlertScheduler {
    fun start()
    fun stop()
}

/** The "Friend alerts" menu switch. */
class FriendAlertSwitch(private val store: FriendAlertsRepository, private val scheduler: AlertScheduler) {

    val enabled: Flow<Boolean> = store.enabled

    /**
     * Switching on remembers who's online right now ([onlineNow], lower-case names; see [onlineAtSwitchOn]), so
     * friends you can already see don't trigger an alert. Null (not known yet) leaves no list, so the first check
     * only records one. Switching off stops the checks and forgets the list.
     */
    suspend fun set(on: Boolean, onlineNow: Set<String>?) {
        store.setLastOnline(if (on) onlineNow else null)
        store.setEnabled(on)
        if (on) scheduler.start() else scheduler.stop()
    }

    /** On app start: the switch is saved with the app's settings, the scheduled checks aren't (e.g. restored backups). */
    suspend fun ensureRunning() {
        if (store.enabled.first()) scheduler.start()
    }
}

/** One background check: who among starred pilots and squads came online since last time. */
class FriendAlertCheck(
    private val players: PlayersSource,
    private val friends: FriendsRepository,
    private val squads: SquadsRepository,
    private val store: FriendAlertsRepository,
    private val notify: (List<RosterRow>) -> Unit,
    private val inForeground: () -> Boolean,
    /** False when notifications or the friend alerts channel are blocked: then a check would be wasted. */
    private val canNotify: () -> Boolean
) {
    suspend fun run() {
        if (!store.enabled.first() || !canNotify()) return
        val friendNames = friends.friends.first()
        val squadTags = squads.squads.first()
        if (friendNames.isEmpty() && squadTags.isEmpty()) return // nobody to look for, so no request
        val roster = try {
            RosterBuilder.build(players.fetch(), friendNames, squadTags)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CBOnline", "Friend alert check failed", e)
            return // try again at the next check, with the old list
        }
        // An empty server (between missions, or a hiccup) says nothing about who left, so it's treated like a failed
        // check: forgetting everyone would alert for all of them again once it fills. The cost: a friend who was on
        // just before it emptied and is among the first back isn't alerted.
        if (roster.total == 0) return
        val online = roster.friendsOnline
        val previous = store.lastOnline.first()
        store.setLastOnline(online.map { nameKey(it.nickname) }.toSet())
        if (previous == null) return // first check after switching on: just record who's there
        val newly = newlyOnline(previous, online)
        // With the app open you can see the list, so the check only keeps its memory up to date
        if (newly.isNotEmpty() && !inForeground()) notify(newly)
    }
}
