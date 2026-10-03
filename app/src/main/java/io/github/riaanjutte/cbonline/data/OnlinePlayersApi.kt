package io.github.riaanjutte.cbonline.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/** Fetches the Combat Box online roster. Throws on network, HTTP or payload failure. */
class OnlinePlayersApi(
    private val client: OkHttpClient,
    private val json: Json,
    private val baseUrl: String,
    private val userAgent: String,
    private val log: (String) -> Unit = {}
) : PlayersSource {

    override suspend fun fetch(): List<OnlinePlayer> = withContext(Dispatchers.IO) {
        log("fetch OnlinePlayers")
        val request = Request.Builder()
            .url("$baseUrl/api/OnlinePlayers")
            .header("User-Agent", userAgent)
            .build()
        val body = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.string().orEmpty()
        }
        json.decodeFromString<List<OnlinePlayerDto>>(body).mapNotNull { it.toModel() }
    }

    @Serializable
    private data class OnlinePlayerDto(
        val nickname: String? = null,
        val coalition: Int? = null,
        val timeOnMission: String? = null
    ) {
        fun toModel(): OnlinePlayer? {
            val name = nickname?.trim().orEmpty()
            if (name.isEmpty()) return null
            val side = when (coalition) {
                1 -> Coalition.Allied
                2 -> Coalition.Axis
                else -> Coalition.Unassigned
            }
            return OnlinePlayer(name, side, timeOnMission.orEmpty())
        }
    }
}
