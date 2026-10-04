package io.github.riaanjutte.cbonline.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.PilotStats
import io.github.riaanjutte.cbonline.roster.isSquadMember
import io.github.riaanjutte.cbonline.roster.isValidSquadTag
import io.github.riaanjutte.cbonline.roster.suggestSquadTag
import io.github.riaanjutte.cbonline.roster.tidyAircraftName
import io.github.riaanjutte.cbonline.ui.brand.BrandLabel
import io.github.riaanjutte.cbonline.ui.theme.CbColors
import io.github.riaanjutte.cbonline.ui.theme.CbText

import java.util.Locale

/** A pilot's stats, sliding up over the roster; [state] must not be [StatsUiState.Hidden]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilotStatsSheet(
    state: StatsUiState,
    starredSquads: Set<String>,
    onRetry: () -> Unit,
    onToggleSquad: (String) -> Unit,
    onClose: () -> Unit
) {
    val nickname = when (state) {
        StatsUiState.Hidden -> return
        is StatsUiState.Loading -> state.nickname
        is StatsUiState.Loaded -> state.nickname
        is StatsUiState.NotFound -> state.nickname
        is StatsUiState.Failed -> state.nickname
    }
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CbColors.Panel
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(nickname, style = CbText.Countdown, color = CbColors.Text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            when (state) {
                is StatsUiState.Loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = CbColors.Amber)
                }
                is StatsUiState.Loaded -> StatsContent(state.stats)
                is StatsUiState.NotFound -> Text(stringResource(R.string.stats_not_found), style = MaterialTheme.typography.bodyMedium, color = CbColors.Muted)
                is StatsUiState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.stats_failed).uppercase(Locale.ROOT), style = CbText.ErrorTitle, color = CbColors.Text)
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = CbColors.Red, contentColor = Color.White)
                    ) { Text(stringResource(R.string.retry).uppercase(Locale.ROOT), style = CbText.Button) }
                }
                StatsUiState.Hidden -> Unit
            }
            HorizontalDivider(color = CbColors.PanelBorder)
            SquadButton(nickname, starredSquads, onToggleSquad)
        }
    }
}

@Composable
private fun StatsContent(stats: PilotStats) {
    val fmt = remember { StatsFormat() }
    val airStreak = stringResource(R.string.stats_best_streak, fmt.whole(stats.bestAirStreak))
    val groundStreak = stringResource(R.string.stats_best_streak, fmt.whole(stats.bestGroundStreak))

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        BrandLabel(stringResource(R.string.stats_lifetime), color = CbColors.Amber)
        StatPair(
            Stat(fmt.oneDecimal(stats.flightHours), stringResource(R.string.stats_flight_hours)),
            Stat(fmt.whole(stats.score), stringResource(R.string.stats_score))
        )
        StatPair(
            Stat(fmt.whole(stats.airKills), stringResource(R.string.stats_air_kills), airStreak),
            Stat(fmt.whole(stats.groundKills), stringResource(R.string.stats_ground_kills), groundStreak)
        )
        StatPair(
            Stat(fmt.whole(stats.deaths), stringResource(R.string.stats_deaths)),
            Stat(fmt.whole(stats.aircraftLost), stringResource(R.string.stats_aircraft_lost))
        )
        StatPair(
            Stat(fmt.twoDecimals(stats.airKillsPerAircraftLost), stringResource(R.string.stats_kills_per_loss)),
            Stat(fmt.twoDecimals(stats.airKillsPerHour), stringResource(R.string.stats_kills_per_hour))
        )
        Column {
            BrandLabel(stringResource(R.string.stats_sorties))
            Text(
                stringResource(R.string.stats_sorties_split, fmt.whole(stats.alliedSorties), fmt.whole(stats.axisSorties)),
                style = MaterialTheme.typography.bodyMedium,
                color = CbColors.Text
            )
        }
        if (stats.topAircraft.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BrandLabel(stringResource(R.string.stats_top_aircraft))
                stats.topAircraft.forEach { aircraft ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            tidyAircraftName(aircraft.name),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CbColors.Text,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            stringResource(R.string.stats_hours, fmt.oneDecimal(aircraft.hours)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = CbColors.Muted
                        )
                    }
                }
            }
        }
        stats.vsPlayers?.let { pvp ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BrandLabel(stringResource(R.string.stats_vs_players))
                Text(
                    stringResource(R.string.stats_pvp_line, fmt.whole(pvp.victories), fmt.whole(pvp.defeats), fmt.twoDecimals(pvp.airToAirRatio)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CbColors.Text
                )
                Text(
                    stringResource(R.string.stats_ai_victories, fmt.whole(pvp.aiVictories)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CbColors.Muted
                )
            }
        }
    }
}

private class Stat(val value: String, val label: String, val detail: String? = null)

@Composable
private fun StatPair(left: Stat, right: Stat) {
    Row(Modifier.fillMaxWidth()) {
        StatCell(left, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        StatCell(right, Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(stat: Stat, modifier: Modifier) {
    Column(modifier) {
        Text(stat.value, style = CbText.MissionName, color = CbColors.Text)
        BrandLabel(stat.label)
        stat.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = CbColors.Muted) }
    }
}

/** Unstar the starred squad this pilot belongs to, or star one, starting from the tag in their name. */
@Composable
private fun SquadButton(nickname: String, starredSquads: Set<String>, onToggleSquad: (String) -> Unit) {
    var dialogOpen by rememberSaveable(nickname) { mutableStateOf(false) }
    val member = starredSquads.sortedWith(String.CASE_INSENSITIVE_ORDER).firstOrNull { isSquadMember(nickname, it) }
    val suggestion = suggestSquadTag(nickname)
    val label = when {
        member != null -> stringResource(R.string.squad_unstar_tag, member)
        suggestion != null -> stringResource(R.string.squad_star_tag, suggestion)
        else -> stringResource(R.string.squad_star)
    }
    OutlinedButton(
        onClick = { if (member != null) onToggleSquad(member) else dialogOpen = true },
        border = BorderStroke(1.dp, CbColors.Amber),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label.uppercase(Locale.ROOT), style = CbText.Button, color = CbColors.Amber, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    if (dialogOpen) {
        SquadDialog(
            initial = suggestion.orEmpty(),
            onConfirm = { onToggleSquad(it.trim()); dialogOpen = false },
            onDismiss = { dialogOpen = false }
        )
    }
}

@Composable
private fun SquadDialog(initial: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var tag by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CbColors.Panel,
        title = { Text(stringResource(R.string.squad_dialog_title).uppercase(Locale.ROOT), style = CbText.MissionName, color = CbColors.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.squad_dialog_text), style = MaterialTheme.typography.bodyMedium, color = CbColors.Muted)
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text(stringResource(R.string.squad_dialog_label)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CbColors.Amber,
                        focusedLabelColor = CbColors.Amber,
                        cursorColor = CbColors.Amber,
                        focusedTextColor = CbColors.Text,
                        unfocusedTextColor = CbColors.Text
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tag) }, enabled = isValidSquadTag(tag)) {
                Text(stringResource(R.string.squad_dialog_confirm).uppercase(Locale.ROOT), style = CbText.Button, color = if (isValidSquadTag(tag)) CbColors.Amber else CbColors.Muted)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel).uppercase(Locale.ROOT), style = CbText.Button, color = CbColors.Muted)
            }
        }
    )
}
