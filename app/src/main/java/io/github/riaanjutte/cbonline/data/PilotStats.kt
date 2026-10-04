package io.github.riaanjutte.cbonline.data

/** A pilot's lifetime record on Combat Box. "Red" sorties are flown for the Allies, "blue" for the Axis. */
data class PilotStats(
    val nickname: String,
    val flightHours: Double,
    val score: Int,
    val alliedSorties: Int,
    val axisSorties: Int,
    val airKills: Int,
    val bestAirStreak: Int,
    val groundKills: Int,
    val bestGroundStreak: Int,
    val deaths: Int,
    val aircraftLost: Int,
    val airKillsPerAircraftLost: Double,
    val airKillsPerHour: Double,
    /** Most-flown aircraft, most hours first, with their names as the feed spells them ("bf 109 g-14"). */
    val topAircraft: List<AircraftHours>,
    /** Null when the PvP record is missing or couldn't be loaded; the rest still shows. */
    val vsPlayers: PvpRecord?
)

data class AircraftHours(val name: String, val hours: Double)

data class PvpRecord(val victories: Int, val defeats: Int, val airToAirRatio: Double, val aiVictories: Int)

sealed interface StatsResult {
    data class Found(val stats: PilotStats) : StatsResult
    /** Combat Box has no stats under this name (new pilot, or a name that changed). */
    data object NotFound : StatsResult
}

interface StatsSource {
    /** Throws on network, HTTP or payload failure of the main record. */
    suspend fun fetch(nickname: String): StatsResult
}
