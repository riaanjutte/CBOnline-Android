package io.github.riaanjutte.cbonline.ui

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MissionClockTest {

    /** Collects the clock with the test's virtual time as "now". */
    private fun TestScope.collect(started: MutableStateFlow<Boolean>): List<Instant> {
        val ticks = mutableListOf<Instant>()
        backgroundScope.launch {
            tickingClock(started, { Instant.ofEpochMilli(testScheduler.currentTime) }).collect { ticks += it }
        }
        runCurrent()
        return ticks
    }

    @Test
    fun `ticks immediately and every 30 seconds while started`() = runTest {
        val ticks = collect(MutableStateFlow(true))
        assertEquals(listOf(Instant.ofEpochMilli(0)), ticks)
        advanceTimeBy(30_001)
        assertEquals(listOf(Instant.ofEpochMilli(0), Instant.ofEpochMilli(30_000)), ticks)
    }

    @Test
    fun `no ticks while stopped and a fresh tick on restart`() = runTest {
        val started = MutableStateFlow(true)
        val ticks = collect(started)
        started.value = false // app in the background / phone asleep
        advanceTimeBy(120_000)
        assertEquals(listOf(Instant.ofEpochMilli(0)), ticks)

        started.value = true // back in the foreground: the label must use the current time at once
        runCurrent()
        assertEquals(listOf(Instant.ofEpochMilli(0), Instant.ofEpochMilli(120_000)), ticks)
    }
}
