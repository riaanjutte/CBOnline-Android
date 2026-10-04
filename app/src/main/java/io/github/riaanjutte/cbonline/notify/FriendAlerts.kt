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
    val lastOnline: Flow<Set<String>>
    suspend fun setEnabled(on: Boolean)
    suspend fun setLastOnline(keys: Set<String>)
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
     * see don't trigger an alert; switching off stops the checks and forgets the list.
     */
    suspend fun set(on: Boolean, onlineNow: Set<String>) {
        store.setLastOnline(if (on) onlineNow else emptySet())
        store.setEnabled(on)
        if (on) scheduler.start() else scheduler.stop()
    }
}

/** One background check: who among starred pilots and squads came online since last time. */
class FriendAlertCheck(
    private val players: PlayersSource,
    private val friends: FriendsRepository,
    private val squads: SquadsRepository,
    private val store: FriendAlertsRepository,
    private val notify: (List<RosterRow>) -> Unit,
    private val inForeground: () -> Boolean
) {
    suspend fun run() {
        if (!store.enabled.first()) return
        val online = try {
            RosterBuilder.build(players.fetch(), friends.friends.first(), squads.squads.first()).friendsOnline
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CBOnline", "Friend alert check failed", e)
            return // try again at the next check, with the old list
        }
        val newly = newlyOnline(store.lastOnline.first(), online)
        store.setLastOnline(online.map { friendKey(it.nickname) }.toSet())
        // With the app open you can see the list, so the check only keeps its memory up to date
        if (newly.isNotEmpty() && !inForeground()) notify(newly)
    }
}
