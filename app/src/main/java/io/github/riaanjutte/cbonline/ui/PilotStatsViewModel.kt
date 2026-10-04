package io.github.riaanjutte.cbonline.ui

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.riaanjutte.cbonline.data.PilotStats
import io.github.riaanjutte.cbonline.data.StatsResult
import io.github.riaanjutte.cbonline.data.StatsSource
import io.github.riaanjutte.cbonline.data.nameKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** The stats panel; [nickname] is the name as tapped. */
sealed interface StatsUiState {
    data object Hidden : StatsUiState
    data class Loading(val nickname: String) : StatsUiState
    data class Loaded(val nickname: String, val stats: PilotStats) : StatsUiState
    data class NotFound(val nickname: String) : StatsUiState
    data class Failed(val nickname: String) : StatsUiState
}

/**
 * Opens one pilot's stats at a time. Answers are kept for [KEEP_STATS_MILLIS], since stats change after every
 * sortie; failures aren't (including a missing PvP part that failed to load), so opening the pilot again or Retry
 * fetches again.
 */
class PilotStatsViewModel(
    private val source: StatsSource,
    /** Time since boot, so changing the phone's clock doesn't stretch or cut the cache. */
    private val now: () -> Long = SystemClock::elapsedRealtime
) : ViewModel() {

    private val _state = MutableStateFlow<StatsUiState>(StatsUiState.Hidden)
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    private class Cached(val result: StatsResult, val at: Long)

    private val cache = mutableMapOf<String, Cached>()
    private var job: Job? = null

    fun open(nickname: String) {
        job?.cancel()
        val key = nameKey(nickname)
        cache[key]?.takeIf { now() - it.at < KEEP_STATS_MILLIS }?.let {
            _state.value = it.result.toUiState(nickname)
            return
        }
        _state.value = StatsUiState.Loading(nickname)
        job = viewModelScope.launch {
            val shown = try {
                source.fetch(nickname)
                    .also { if (it !is StatsResult.Found || it.complete) cache[key] = Cached(it, now()) }
                    .toUiState(nickname)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("CBOnline", "Stats fetch failed", e)
                StatsUiState.Failed(nickname)
            }
            // An answer (or failure) can arrive just as the panel is closed or another pilot opened; it's dropped then
            ensureActive()
            _state.value = shown
        }
    }

    fun retry() {
        (_state.value as? StatsUiState.Failed)?.let { open(it.nickname) }
    }

    fun close() {
        job?.cancel()
        _state.value = StatsUiState.Hidden
    }

    private fun StatsResult.toUiState(nickname: String) = when (this) {
        is StatsResult.Found -> StatsUiState.Loaded(nickname, stats)
        StatsResult.NotFound -> StatsUiState.NotFound(nickname)
    }
}

private const val KEEP_STATS_MILLIS = 15 * 60_000L
