package io.github.riaanjutte.cbonline.notify

import android.util.Log
import io.github.riaanjutte.cbonline.data.FriendsRepository
import io.github.riaanjutte.cbonline.data.PlayersSource
import io.github.riaanjutte.cbonline.data.SquadsRepository
import io.github.riaanjutte.cbonline.roster.RosterBuilder
import io.github.riaanjutte.cbonline.roster.RosterRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

/** Friends online are remembered by lower-case name, matching how stars ignore case. */
fun friendKey(nickname: String): String = nickname.trim().lowercase(Locale.ROOT)

/** Friends online now who weren't at the last check, each once. */
fun newlyOnline(previous: Set<String>, nowOnline: List<RosterRow>): List<RosterRow> =
    nowOnline.filter { friendKey(it.nickname) !in previous }.distinctBy { friendKey(it.nickname) }

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
     * Switching on remembers who's online right now ([onlineNow], lower-case names), so friends you can already
     * see don't trigger an alert. Null (no roster loaded yet) leaves no list, so the first check only records
     * one. Switching off stops the checks and forgets the list.
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
        val online = try {
            RosterBuilder.build(players.fetch(), friendNames, squadTags).friendsOnline
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CBOnline", "Friend alert check failed", e)
            return // try again at the next check, with the old list
        }
        val previous = store.lastOnline.first()
        store.setLastOnline(online.map { friendKey(it.nickname) }.toSet())
        if (previous == null) return // first check after switching on: just record who's there
        val newly = newlyOnline(previous, online)
        // With the app open you can see the list, so the check only keeps its memory up to date
        if (newly.isNotEmpty() && !inForeground()) notify(newly)
    }
}
