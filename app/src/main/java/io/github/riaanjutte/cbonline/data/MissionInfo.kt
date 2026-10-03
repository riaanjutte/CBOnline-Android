package io.github.riaanjutte.cbonline.data

import java.time.Instant
import java.time.LocalDateTime

/** The mission currently running on the server, plus what's coming next. */
data class MissionInfo(
    val name: String,
    /** In-game date/time the mission is set at (no time zone). */
    val historicalStart: LocalDateTime?,
    val estimatedEnd: Instant,
    val weather: Weather?,
    val next: NextMission?
)

data class NextMission(
    val name: String,
    val expectedStart: Instant,
    val historicalStart: LocalDateTime?,
    val weather: Weather?
)

data class Weather(
    val temperatureC: Double?,
    val cloudCover: String?,
    val cloudBaseM: Int?,
    val cloudTopM: Int?,
    val precipLevel: Double,
    val precipType: String?,
    /** Lowest wind layer. */
    val surfaceWind: Wind?
)

/** Wind as pilots read it: the direction it blows from. */
data class Wind(val fromDeg: Int, val speedMs: Double)

interface MissionSource {
    suspend fun fetch(): MissionInfo
}
