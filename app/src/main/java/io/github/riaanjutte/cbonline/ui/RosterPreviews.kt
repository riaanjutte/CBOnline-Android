package io.github.riaanjutte.cbonline.ui

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.data.OnlinePlayer
import io.github.riaanjutte.cbonline.data.UpdateInfo
import io.github.riaanjutte.cbonline.roster.RosterBuilder
import io.github.riaanjutte.cbonline.ui.theme.CbOnlineTheme
import java.time.Instant

private val samplePlayers = listOf(
    OnlinePlayer("Bravo", Coalition.Axis, "01:05"),
    OnlinePlayer("=JG52=Hans", Coalition.Axis, "00:27"),
    OnlinePlayer("Alpha", Coalition.Allied, "00:12"),
    OnlinePlayer("[CB]Ace", Coalition.Allied, "00:02"),
    OnlinePlayer("Charlie", Coalition.Allied, "00:31"),
    OnlinePlayer("Spectator", Coalition.Unassigned, "00:04")
)
private val sampleTime = Instant.parse("2026-10-03T19:23:00Z")

private fun loaded(friends: Set<String> = setOf("alpha", "Bravo", "Dave"), update: UpdateInfo? = null) = RosterUiState(
    roster = RosterBuilder.build(samplePlayers, friends),
    lastUpdated = sampleTime,
    isLoading = false,
    update = update
)

@Composable
private fun Preview(state: RosterUiState) = CbOnlineTheme {
    RosterScreen(state, "1.0.0", onRefresh = {}, onToggleFriend = {}, onDismissUpdate = {}, onOpenUrl = {})
}

@Preview(showBackground = true)
@Composable
private fun LoadingPreview() = Preview(RosterUiState())

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoadedPreview() = Preview(loaded(update = UpdateInfo("1.1.0", "https://example.invalid")))

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
