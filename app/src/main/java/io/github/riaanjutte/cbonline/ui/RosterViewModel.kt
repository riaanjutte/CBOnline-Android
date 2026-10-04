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
import io.github.riaanjutte.cbonline.data.UpdateInfo
import io.github.riaanjutte.cbonline.data.UpdateSource
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
    val errorMessage: String? = null,
    val update: UpdateInfo? = null,
    /** Last good mission info; independent of the roster's error state. */
    val mission: MissionInfo? = null,
    /** Starred squad tags, for the stats panel's Star / Unstar squad button. */
    val squads: Set<String> = emptySet()
)

class RosterViewModel(
    private val players: PlayersSource,
    private val missions: MissionSource,
    private val friends: FriendsRepository,
    private val squads: SquadsRepository,
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
        val errorMessage: String? = null
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
        combine(fetchState, friends.friends, squads.squads, updateState, missionState, ::toUiState).collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RosterUiState())

    fun refresh() {
        if (!fetchState.value.inFlight) manualRefresh.trySend(Unit)
    }

    fun toggleFriend(nickname: String) {
        viewModelScope.launch { persist("Saving friend failed") { friends.toggle(nickname) } }
    }

    fun toggleSquad(tag: String) {
        viewModelScope.launch { persist("Saving squad failed") { squads.toggle(tag) } }
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
            missionState.value = missions.fetch()
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
                it.copy(players = result, lastUpdated = Instant.now(), isLoading = false, refreshFailed = false, errorMessage = null)
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
                else it.copy(isLoading = false, errorMessage = e.message ?: e::class.simpleName)
            }
        }
    }

    private fun toUiState(
        fetch: FetchState,
        friendNames: Set<String>,
        squadTags: Set<String>,
        update: UpdateInfo?,
        mission: MissionInfo?
    ) = RosterUiState(
        roster = fetch.players?.let { RosterBuilder.build(it, friendNames, squadTags) },
        squads = squadTags,
        lastUpdated = fetch.lastUpdated,
        isLoading = fetch.isLoading,
        isRefreshing = fetch.isRefreshing,
        refreshFailed = fetch.refreshFailed,
        errorMessage = fetch.errorMessage,
        update = update,
        mission = mission
    )

    private companion object {
        const val REFRESH_INTERVAL_MS = 60_000L
    }
}
