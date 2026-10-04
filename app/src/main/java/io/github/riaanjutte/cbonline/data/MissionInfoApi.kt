package io.github.riaanjutte.cbonline.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.roundToInt

/** Fetches Combat Box's mission-info file. Throws on network, HTTP or payload failure. */
class MissionInfoApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val url: String,
    private val userAgent: String,
    private val log: (String) -> Unit = {}
) : MissionSource {

    override suspend fun fetch(): MissionInfo = withContext(Dispatchers.IO) {
        log("fetch MissionInfo")
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
        json.decodeFromString<MissionInfoDto>(body).toModel()
    }

    @Serializable
    private data class MissionInfoDto(
        val mission: MissionDto? = null,
        val weather: WeatherDto? = null,
        @SerialName("next_mission") val nextMission: NextMissionDto? = null
    ) {
        fun toModel(): MissionInfo {
            val current = requireNotNull(mission) { "no current mission" }
            val name = current.name?.trim().orEmpty()
            require(name.isNotEmpty()) { "mission has no name" }
            val end = requireNotNull(parseInstant(current.estimatedEnd)) { "bad estimated_end: ${current.estimatedEnd}" }
            return MissionInfo(
                name, parseLocal(current.historicalStart), end, weather?.toModel(), nextMission?.toModel(),
                startedAt = parseInstant(current.startedAt)
            )
        }
    }

    @Serializable
    private data class MissionDto(
        val name: String? = null,
        @SerialName("historical_start") val historicalStart: String? = null,
        @SerialName("started_at") val startedAt: String? = null,
        @SerialName("estimated_end") val estimatedEnd: String? = null
    )

    @Serializable
    private data class NextMissionDto(
        val name: String? = null,
        @SerialName("expected_start") val expectedStart: String? = null,
        @SerialName("historical_start") val historicalStart: String? = null,
        val weather: WeatherDto? = null
    ) {
        // A next mission without a name or start time can't be shown meaningfully
        fun toModel(): NextMission? {
            val name = name?.trim().orEmpty().ifEmpty { return null }
            val start = parseInstant(expectedStart) ?: return null
            return NextMission(name, start, parseLocal(historicalStart), weather?.toModel())
        }
    }

    // Numbers are read as Double: the feed is generated and its numeric types can drift
    @Serializable
    private data class WeatherDto(
        @SerialName("temperature_c") val temperatureC: Double? = null,
        @SerialName("cloud_cover") val cloudCover: String? = null,
        @SerialName("cloud_base_m") val cloudBaseM: Double? = null,
        @SerialName("cloud_top_m") val cloudTopM: Double? = null,
        @SerialName("precip_level") val precipLevel: Double = 0.0,
        @SerialName("precip_type") val precipType: String? = null,
        @SerialName("wind_layers") val windLayers: List<WindLayerDto> = emptyList()
    ) {
        fun toModel() = Weather(
            temperatureC = temperatureC,
            cloudCover = cloudCover,
            cloudBaseM = cloudBaseM?.roundToInt(),
            cloudTopM = cloudTopM?.roundToInt(),
            precipLevel = precipLevel,
            precipType = precipType,
            surfaceWind = windLayers.minByOrNull { it.altitudeM }?.let { Wind(it.directionFromDeg.roundToInt(), it.speedMs) }
        )
    }

    @Serializable
    private data class WindLayerDto(
        @SerialName("altitude_m") val altitudeM: Double = 0.0,
        @SerialName("direction_from_deg") val directionFromDeg: Double = 0.0,
        @SerialName("speed_ms") val speedMs: Double = 0.0
    )

    private companion object {
        fun parseInstant(value: String?): Instant? = value?.let { runCatching { Instant.parse(it) }.getOrNull() }
        fun parseLocal(value: String?): LocalDateTime? = value?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
    }
}
