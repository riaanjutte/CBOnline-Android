package io.github.riaanjutte.cbonline.ui

import io.github.riaanjutte.cbonline.MainDispatcherRule
import io.github.riaanjutte.cbonline.data.PilotStats
import io.github.riaanjutte.cbonline.data.StatsResult
import io.github.riaanjutte.cbonline.data.StatsSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class PilotStatsViewModelTest {

    @get:Rule
    val main = MainDispatcherRule()

    private class FakeStats : StatsSource {
        val calls = mutableListOf<String>()
        val gates = mutableMapOf<String, CompletableDeferred<Unit>>()
        var answer: (String) -> StatsResult = { StatsResult.Found(stats(it)) }
        override suspend fun fetch(nickname: String): StatsResult {
            calls += nickname
            gates[nickname]?.await()
            return answer(nickname)
        }
    }

    private val source = FakeStats()
    private val vm = PilotStatsViewModel(source)

    @Test
    fun `starts hidden`() = assertEquals(StatsUiState.Hidden, vm.state.value)

    @Test
    fun `opening shows loading then the stats`() = runTest {
        source.gates["Hans"] = CompletableDeferred()
        vm.open("Hans")
        runCurrent()
        assertEquals(StatsUiState.Loading("Hans"), vm.state.value)
        source.gates.getValue("Hans").complete(Unit)
        runCurrent()
        assertEquals(StatsUiState.Loaded("Hans", stats("Hans")), vm.state.value)
    }

    @Test
    fun `reopening a pilot uses the session cache, ignoring case`() = runTest {
        vm.open("Hans"); runCurrent()
        vm.close()
        vm.open(" hans "); runCurrent()
        assertEquals(listOf("Hans"), source.calls)
        assertEquals(StatsUiState.Loaded(" hans ", stats("Hans")), vm.state.value)
    }

    @Test
    fun `not found is shown and cached`() = runTest {
        source.answer = { StatsResult.NotFound }
        vm.open("Nobody"); runCurrent()
        assertEquals(StatsUiState.NotFound("Nobody"), vm.state.value)
        vm.open("Nobody"); runCurrent()
        assertEquals(1, source.calls.size)
    }

    @Test
    fun `a failure can be retried and isn't cached`() = runTest {
        source.answer = { throw IOException("offline") }
        vm.open("Hans"); runCurrent()
        assertEquals(StatsUiState.Failed("Hans"), vm.state.value)
        source.answer = { StatsResult.Found(stats(it)) }
        vm.retry(); runCurrent()
        assertEquals(StatsUiState.Loaded("Hans", stats("Hans")), vm.state.value)
        assertEquals(2, source.calls.size)
    }

    @Test
    fun `opening another pilot drops the first one's late answer`() = runTest {
        source.gates["Slow"] = CompletableDeferred()
        vm.open("Slow"); runCurrent()
        vm.open("Fast"); runCurrent()
        source.gates.getValue("Slow").complete(Unit); runCurrent()
        assertEquals(StatsUiState.Loaded("Fast", stats("Fast")), vm.state.value)
    }

    @Test
    fun `closing hides the panel and a late answer doesn't reopen it`() = runTest {
        source.gates["Hans"] = CompletableDeferred()
        vm.open("Hans"); runCurrent()
        vm.close()
        source.gates.getValue("Hans").complete(Unit); runCurrent()
        assertEquals(StatsUiState.Hidden, vm.state.value)
    }

    @Test
    fun `retry does nothing unless the last attempt failed`() = runTest {
        vm.retry(); runCurrent()
        assertEquals(StatsUiState.Hidden, vm.state.value)
        assertEquals(0, source.calls.size)
    }

    private companion object {
        fun stats(name: String) = PilotStats(
            nickname = name, flightHours = 1.0, score = 1, alliedSorties = 1, axisSorties = 1, airKills = 1, bestAirStreak = 1,
            groundKills = 1, bestGroundStreak = 1, deaths = 1, aircraftLost = 1, airKillsPerAircraftLost = 1.0,
            airKillsPerHour = 1.0, topAircraft = emptyList(), vsPlayers = null
        )
    }
}
