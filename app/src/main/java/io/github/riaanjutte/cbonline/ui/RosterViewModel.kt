package io.github.riaanjutte.cbonline.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.riaanjutte.cbonline.data.FriendsRepository
import io.github.riaanjutte.cbonline.data.MissionInfo
import io.github.riaanjutte.cbonline.data.MissionSource
import io.github.riaanjutte.cbonline.data.OnlinePlayer
import io.github.riaanjutte.cbonline.data.PlayersSource
import io.github.riaanjutte.cbonline.data.SquadsRepository
import io.github.riaanjutte.cbonline.notify.FriendAlertSwitch
import io.github.riaanjutte.cbonline.notify.MissionReminder
import io.github.riaanjutte.cbonline.notify.MissionReminders
import io.github.riaanjutte.cbonline.data.UpdateInfo
import io.github.riaanjutte.cbonline.data.UpdateSource
import io.github.riaanjutte.cbonline.data.nameKey
import io.github.riaanjutte.cbonline.roster.Roster
import io.github.riaanjutte.cbonline.roster.RosterBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.cancellation.CancellationException

data class RosterUiState(
    val roster: Roster? = null,
    val lastUpdated: Instant? = null,
    val isLoading: Boolean = true,
    /** True only while a pull-to-refresh is in flight, not during automatic polls. */
    val isRefreshing: Boolean = false,
    /** A refresh failed while older data is still on screen. */
    val refreshFailed: Boolean = false,
    /** Set only when there is no roster to show. */
    val loadError: LoadError? = null,
    val update: UpdateInfo? = null,
    /** Last good mission info; independent of the roster's error state. */
    val mission: MissionInfo? = null,
    /** Starred squad tags, for the stats panel's Star / Unstar squad button. */
    val squads: Set<String> = emptySet(),
    /** The pending next-mission reminder (the bell on the mission card). */
    val reminder: MissionReminder? = null,
    val friendAlertsOn: Boolean = false
)

/**
 * Why the first load failed, worded by the screen. Never the exception's own text: that can name the server
 * ("Unable to resolve host …"), and the app shows no server addresses.
 */
sealed interface LoadError {
    /** No network, the server unreachable, or it took too long. */
    data object Offline : LoadError
    data class Server(val httpCode: Int) : LoadError
    data object Other : LoadError

    companion object {
        private val HTTP = Regex("""^HTTP (\d{3})$""")

        fun from(e: Throwable): LoadError = when {
            e is IOException -> e.message?.let { HTTP.matchEntire(it) }?.let { Server(it.groupValues[1].toInt()) } ?: Offline
            else -> Other
        }
    }
}

class RosterViewModel(
    private val players: PlayersSource,
    private val missions: MissionSource,
    private val friends: FriendsRepository,
    private val squads: SquadsRepository,
    private val reminders: MissionReminders,
    private val friendAlerts: FriendAlertSwitch,
    private val updates: UpdateSource,
    private val currentVersion: String
) : ViewModel() {

    /** Survives unsubscribe/resubscribe so the last roster stays on screen. */
    private data class FetchState(
        val players: List<OnlinePlayer>? = null,
        val lastUpdated: Instant? = null,
        val inFlight: Boolean = false,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val refreshFailed: Boolean = false,
        val loadError: LoadError? = null
    )

    private val fetchState = MutableStateFlow(FetchState())
    private val updateState = MutableStateFlow<UpdateInfo?>(null)
    private val missionState = MutableStateFlow<MissionInfo?>(null)
    private val manualRefresh = Channel<Unit>(Channel.CONFLATED)
    private var updateChecked = false

    val state: StateFlow<RosterUiState> = channelFlow {
        launch { // polls only while the UI collects
            manualRefresh.tryReceive() // drop a refresh requested while not visible
            var manual = false
            while (true) {
                fetchOnce(manual)
                manual = withTimeoutOrNull(REFRESH_INTERVAL_MS) { manualRefresh.receive() } != null
            }
        }
        val prefs = combine(friends.friends, squads.squads, reminders.reminder, friendAlerts.enabled, ::Prefs)
        combine(fetchState, prefs, updateState, missionState, ::toUiState).collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RosterUiState())

    fun refresh() {
        if (!fetchState.value.inFlight) manualRefresh.trySend(Unit)
    }

    fun toggleFriend(nickname: String) {
        viewModelScope.launch { persist("Saving friend failed") { friends.toggle(nickname) } }
    }

    fun starSquad(tag: String) {
        viewModelScope.launch { persist("Saving squad failed") { squads.add(tag) } }
    }

    fun unstarSquad(tag: String) {
        viewModelScope.launch { persist("Saving squad failed") { squads.remove(tag) } }
    }

    /** The bell: a reminder before the next mission, or cancel it. Notification permission is the screen's job. */
    fun toggleReminder() {
        val next = missionState.value?.next ?: return
        viewModelScope.launch { persist("Saving reminder failed") { reminders.toggle(next, Instant.now()) } }
    }

    /** Friends already online when alerts are switched on don't alert; [state]'s roster says who they are. */
    fun setFriendAlerts(on: Boolean) {
        // Null when no roster has loaded: then the first background check records who's online instead
        val onlineNow = state.value.roster?.friendsOnline?.map { nameKey(it.nickname) }?.toSet()
        viewModelScope.launch { persist("Saving friend alerts failed") { friendAlerts.set(on, onlineNow) } }
    }

    fun dismissUpdate() {
        val info = updateState.value ?: return
        updateState.value = null
        viewModelScope.launch { persist("Saving dismissal failed") { updates.dismiss(info.version) } }
    }

    /** A failed local write (disk full, corrupt file) must not crash the app; the change just isn't kept. */
    private suspend fun persist(failure: String, write: suspend () -> Unit) {
        try {
            write()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CBOnline", failure, e)
        }
    }

    private suspend fun fetchOnce(manual: Boolean) {
        fetchState.update {
            it.copy(inFlight = true, isLoading = it.players == null, isRefreshing = manual && it.players != null)
        }
        try {
            coroutineScope { // both run concurrently; each updates its own state as soon as it finishes
                launch { fetchMission() }
                fetchRoster() // catches its own failures, so it never cancels the mission fetch
            }
        } finally {
            fetchState.update { it.copy(inFlight = false, isRefreshing = false) }
        }
    }

    /** Keeps the last good mission on any failure; mission problems never touch the roster's state. */
    private suspend fun fetchMission() {
        try {
            val mission = missions.fetch()
            missionState.value = mission
            // A start time that moved, or a different next mission, updates or drops the reminder
            persist("Updating reminder failed") { reminders.reconcile(mission.next, Instant.now()) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("CBOnline", "Mission info fetch failed", e)
        }
    }

    private suspend fun fetchRoster() {
        try {
            val result = players.fetch()
            fetchState.update {
                it.copy(players = result, lastUpdated = Instant.now(), isLoading = false, refreshFailed = false, loadError = null)
            }
            if (!updateChecked) {
                updateChecked = true
                viewModelScope.launch { updateState.value = updates.check(currentVersion) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fetchState.update {
                if (it.players != null) it.copy(isLoading = false, refreshFailed = true)
                else it.copy(isLoading = false, loadError = LoadError.from(e))
            }
        }
    }

    /** Saved choices that shape the screen. */
    private data class Prefs(
        val friendNames: Set<String>,
        val squadTags: Set<String>,
        val reminder: MissionReminder?,
        val friendAlertsOn: Boolean
    )

    private fun toUiState(fetch: FetchState, prefs: Prefs, update: UpdateInfo?, mission: MissionInfo?) = RosterUiState(
        roster = fetch.players?.let { RosterBuilder.build(it, prefs.friendNames, prefs.squadTags) },
        squads = prefs.squadTags,
        reminder = prefs.reminder,
        friendAlertsOn = prefs.friendAlertsOn,
        lastUpdated = fetch.lastUpdated,
        isLoading = fetch.isLoading,
        isRefreshing = fetch.isRefreshing,
        refreshFailed = fetch.refreshFailed,
        loadError = fetch.loadError,
        update = update,
        mission = mission
    )

    private companion object {
        const val REFRESH_INTERVAL_MS = 60_000L
    }
}
