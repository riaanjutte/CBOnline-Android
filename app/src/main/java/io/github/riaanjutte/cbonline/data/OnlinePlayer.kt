package io.github.riaanjutte.cbonline.data

enum class Coalition { Axis, Allied, Unassigned }

data class OnlinePlayer(
    val nickname: String,
    val coalition: Coalition,
    /** Time since joining the mission, as sent by the API ("HH:MM"). */
    val timeOnMission: String
)

interface PlayersSource {
    suspend fun fetch(): List<OnlinePlayer>
}
