package io.github.riaanjutte.cbonline.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.riaanjutte.cbonline.data.PilotStats
import io.github.riaanjutte.cbonline.data.StatsResult
import io.github.riaanjutte.cbonline.data.StatsSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
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
 * Opens one pilot's stats at a time. Answers are kept for the session; failures aren't (including a missing PvP
 * part that failed to load), so opening the pilot again or Retry fetches again.
 */
class PilotStatsViewModel(private val source: StatsSource) : ViewModel() {

    private val _state = MutableStateFlow<StatsUiState>(StatsUiState.Hidden)
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    private val cache = mutableMapOf<String, StatsResult>()
    private var job: Job? = null

    fun open(nickname: String) {
        job?.cancel()
        val key = nickname.trim().lowercase(Locale.ROOT)
        cache[key]?.let {
            _state.value = it.toUiState(nickname)
            return
        }
        _state.value = StatsUiState.Loading(nickname)
        job = viewModelScope.launch {
            _state.value = try {
                source.fetch(nickname)
                    .also { if (it !is StatsResult.Found || it.complete) cache[key] = it }
                    .toUiState(nickname)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("CBOnline", "Stats fetch failed", e)
                StatsUiState.Failed(nickname)
            }
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
