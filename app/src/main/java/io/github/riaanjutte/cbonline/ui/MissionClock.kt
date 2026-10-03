package io.github.riaanjutte.cbonline.ui

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Instant

/**
 * Emits [now] at once and then every [periodMillis] while [started] is true, and stops otherwise.
 * Every restart emits a fresh time immediately: delays don't advance while the phone sleeps, so a
 * free-running timer would show a stale countdown for up to a whole period after unlocking.
 */
fun tickingClock(started: Flow<Boolean>, now: () -> Instant, periodMillis: Long = 30_000): Flow<Instant> = channelFlow {
    started.distinctUntilChanged().collectLatest { isStarted ->
        while (isStarted) {
            send(now())
            delay(periodMillis)
        }
    }
}
