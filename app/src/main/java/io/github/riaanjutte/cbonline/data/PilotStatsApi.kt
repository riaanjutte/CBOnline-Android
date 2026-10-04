package io.github.riaanjutte.cbonline.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import kotlin.math.roundToInt

/** Fetches a pilot's lifetime and PvP records by in-game name, both at once. */
class PilotStatsApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: String,
    private val userAgent: String,
    private val log: (String) -> Unit = {}
) : StatsSource {

    override suspend fun fetch(nickname: String): StatsResult = withContext(Dispatchers.IO) {
        log("fetch stats")
        coroutineScope {
            val pvp = async { fetchPvp(nickname) }
            val lifetime = json.decodeFromString<LifetimeDto>(get("LifetimeStats", nickname))
            val row = lifetime.lifetime?.firstOrNull()
            if (row == null) {
                pvp.cancel()
                return@coroutineScope StatsResult.NotFound
            }
            StatsResult.Found(row.toModel(nickname, lifetime.top.orEmpty(), pvp.await()))
        }
    }

    /** The PvP record is extra: missing or failing, it just leaves that part out. */
    private fun fetchPvp(nickname: String): PvpRecord? = try {
        json.decodeFromString<PvpDto>(get("PvpStats", nickname)).toModel()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log("PvP stats unavailable: ${e.message}")
        null
    }

    private fun get(endpoint: String, nickname: String): String {
        val url = "$baseUrl/api/$endpoint".toHttpUrl().newBuilder().addQueryParameter("name", nickname).build()
        val request = Request.Builder().url(url).header("User-Agent", userAgent).build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
    }

    // The feed sends counts as JSON numbers that may carry a decimal point, so everything is read as Double
    @Serializable
    private data class LifetimeDto(
        @SerialName("LifetimeStats") val lifetime: List<LifetimeRowDto>? = null,
        @SerialName("Top 3 aircraft") val top: List<AircraftDto>? = null
    )

    @Serializable
    private data class LifetimeRowDto(
        val nickname: String? = null,
        @SerialName("total_flight_time_hours") val hours: Double? = null,
        @SerialName("total_score") val score: Double? = null,
        @SerialName("red_sorties") val redSorties: Double? = null,
        @SerialName("blue_sorties") val blueSorties: Double? = null,
        @SerialName("total_air_kills") val airKills: Double? = null,
        @SerialName("max_air_kill_streak") val bestAirStreak: Double? = null,
        @SerialName("total_ground_kills") val groundKills: Double? = null,
        @SerialName("max_ground_kill_streak") val bestGroundStreak: Double? = null,
        @SerialName("total_deaths_plus_captures") val deaths: Double? = null,
        @SerialName("lost_aircraft") val lostAircraft: Double? = null,
        @SerialName("air_kills_per_lost_aircraft") val airKillsPerLost: Double? = null,
        @SerialName("air_kills_per_hour") val airKillsPerHour: Double? = null
    ) {
        fun toModel(requested: String, top: List<AircraftDto>, pvp: PvpRecord?) = PilotStats(
            nickname = nickname?.takeIf { it.isNotBlank() } ?: requested,
            flightHours = hours ?: 0.0,
            score = score.count(),
            alliedSorties = redSorties.count(),
            axisSorties = blueSorties.count(),
            airKills = airKills.count(),
            bestAirStreak = bestAirStreak.count(),
            groundKills = groundKills.count(),
            bestGroundStreak = bestGroundStreak.count(),
            deaths = deaths.count(),
            aircraftLost = lostAircraft.count(),
            airKillsPerAircraftLost = airKillsPerLost ?: 0.0,
            airKillsPerHour = airKillsPerHour ?: 0.0,
            topAircraft = top.mapNotNull { a -> a.name?.takeIf { it.isNotBlank() }?.let { AircraftHours(it, a.hours ?: 0.0) } },
            vsPlayers = pvp
        )
    }

    @Serializable
    private data class AircraftDto(
        @SerialName("log_name") val name: String? = null,
        @SerialName("total_flight_time_hours") val hours: Double? = null
    )

    @Serializable
    private data class PvpDto(
        @SerialName("pvp_victories") val victories: Double? = null,
        @SerialName("pvp_defeats") val defeats: Double? = null,
        @SerialName("pvp_air_to_air_win_loss_ratio") val ratio: Double? = null,
        @SerialName("ai_victories") val aiVictories: Double? = null
    ) {
        /** A "not found" answer has only a message, so none of these are set. */
        fun toModel(): PvpRecord? =
            if (victories == null && defeats == null) null
            else PvpRecord(victories.count(), defeats.count(), ratio ?: 0.0, aiVictories.count())
    }
}

private fun Double?.count(): Int = this?.roundToInt() ?: 0
