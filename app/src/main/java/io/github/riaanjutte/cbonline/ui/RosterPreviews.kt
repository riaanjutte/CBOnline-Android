package io.github.riaanjutte.cbonline.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.data.MissionInfo
import io.github.riaanjutte.cbonline.data.NextMission
import io.github.riaanjutte.cbonline.data.OnlinePlayer
import io.github.riaanjutte.cbonline.data.UpdateInfo
import io.github.riaanjutte.cbonline.data.Weather
import io.github.riaanjutte.cbonline.data.Wind
import io.github.riaanjutte.cbonline.roster.RosterBuilder
import io.github.riaanjutte.cbonline.ui.theme.CbOnlineTheme
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

private val samplePlayers = listOf(
    OnlinePlayer("Bravo", Coalition.Axis, "01:05"),
    OnlinePlayer("=JG52=Hans", Coalition.Axis, "00:27"),
    OnlinePlayer("Alpha", Coalition.Allied, "00:12"),
    OnlinePlayer("[CB]Ace", Coalition.Allied, "00:02"),
    OnlinePlayer("Charlie", Coalition.Allied, "00:31"),
    OnlinePlayer("Spectator", Coalition.Unassigned, "00:04")
)
private val sampleTime = Instant.parse("2026-10-03T19:23:00Z")

private fun sampleMission(
    end: Instant = Instant.now().plus(Duration.ofMinutes(167)),
    name: String = "Crimean Resolve (Dec. 1944)"
) = MissionInfo(
    name, LocalDateTime.of(1944, 12, 2, 14, 0), end,
    Weather(1.0, "Heavy", 1900, 4900, 0.0, null, Wind(160, 3.0)),
    NextMission(
        "Mitchell's Men (Mar. 1945)", end, LocalDateTime.of(1945, 3, 5, 9, 0),
        Weather(-15.0, "Heavy", 3500, 9500, 0.0, null, Wind(280, 4.0))
    )
)

private fun loaded(
    friends: Set<String> = setOf("alpha", "Bravo", "Dave"),
    update: UpdateInfo? = null,
    mission: MissionInfo? = sampleMission()
) = RosterUiState(
    roster = RosterBuilder.build(samplePlayers, friends),
    lastUpdated = sampleTime,
    isLoading = false,
    update = update,
    mission = mission
)

@Composable
private fun Preview(state: RosterUiState) = CbOnlineTheme {
    RosterScreen(state, "1.0.0", onRefresh = {}, onToggleFriend = {}, onToggleSquad = {}, onOpenStats = {}, onDismissUpdate = {}, onOpenUrl = {})
}

@Preview(showBackground = true)
@Composable
private fun LoadingPreview() = Preview(RosterUiState())

@Preview(showBackground = true)
@Composable
private fun LoadedPreview() = Preview(loaded(update = UpdateInfo("1.1.0", "https://example.invalid")))

// Review Focus: large text must wrap and grow inside the cards, not clip
@Preview(showBackground = true, fontScale = 2f)
@Composable
private fun LargeFontPreview() = Preview(loaded())

// Review Focus: the header and list stay usable on a short, wide screen
@Preview(showBackground = true, widthDp = 800, heightDp = 360)
@Composable
private fun LandscapePreview() = Preview(loaded())

// Review Focus: update banner and refresh strip together still leave room for the list
@Preview(showBackground = true, heightDp = 640)
@Composable
private fun AllStripsPreview() = Preview(loaded(update = UpdateInfo("1.1.0", "https://example.invalid")).copy(refreshFailed = true))

@Preview(showBackground = true)
@Composable
private fun EmptyPreview() = Preview(
    RosterUiState(roster = RosterBuilder.build(emptyList(), emptySet()), lastUpdated = sampleTime, isLoading = false)
)

@Preview(showBackground = true)
@Composable
private fun RefreshFailedPreview() = Preview(loaded().copy(refreshFailed = true))

@Preview(showBackground = true)
@Composable
private fun ErrorPreview() = Preview(RosterUiState(isLoading = false, errorMessage = "HTTP 503"))

@Preview(showBackground = true)
@Composable
private fun ChangingMissionPreview() = Preview(loaded(mission = sampleMission(end = Instant.now().minusSeconds(120))))

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun LongMissionNamePreview() = Preview(
    loaded(mission = sampleMission(name = "Operation Bagration Phase II: Breakthrough at Bobruisk (June 1944)"))
)

@Preview(showBackground = true)
@Composable
private fun ErrorWithMissionPreview() = Preview(RosterUiState(isLoading = false, errorMessage = "HTTP 503", mission = sampleMission()))

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun LongNicknamePreview() {
    val longName = "=JG52=Oblt.Hans-Joachim_von_Oberammergau_Staffelkapitaen_III"
    Preview(
        RosterUiState(
            roster = RosterBuilder.build(listOf(OnlinePlayer(longName, Coalition.Axis, "12:00")), setOf(longName)),
            lastUpdated = sampleTime,
            isLoading = false
        )
    )
}
