package io.github.riaanjutte.cbonline.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.MissionInfo
import io.github.riaanjutte.cbonline.data.Weather
import io.github.riaanjutte.cbonline.notify.canRemind
import io.github.riaanjutte.cbonline.roster.inGameLabel
import io.github.riaanjutte.cbonline.roster.keepPartsTogether
import io.github.riaanjutte.cbonline.roster.timeLeftLabel
import io.github.riaanjutte.cbonline.roster.weatherLine
import io.github.riaanjutte.cbonline.ui.brand.BrandLabel
import io.github.riaanjutte.cbonline.ui.brand.panel
import io.github.riaanjutte.cbonline.ui.theme.CbColors
import io.github.riaanjutte.cbonline.ui.theme.CbText
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.util.Date

/**
 * [reminderOn] says whether the bell is set for the next mission; [onToggleReminder] null hides the bell
 * (previews, or no way to remind).
 */
@Composable
fun MissionCard(
    mission: MissionInfo?,
    modifier: Modifier = Modifier,
    reminderOn: Boolean = false,
    onToggleReminder: (() -> Unit)? = null
) {
    if (mission == null) {
        // 1 dp, not 0: a zero-height first item doesn't count as visible, so the list would anchor
        // to the summary card and push this card off-screen once mission data arrives.
        // Deliberately not `modifier`: the caller's padding would leave a gap above the summary.
        Spacer(Modifier.height(1.dp))
        return
    }
    // Local clock for the countdown: ticks while the screen is started, restarts fresh on return, never fetches
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val clock = remember(lifecycle) {
        tickingClock(lifecycle.currentStateFlow.map { it.isAtLeast(Lifecycle.State.STARTED) }, Instant::now)
    }
    val now by clock.collectAsState(initial = Instant.now())
    val context = LocalContext.current
    Column(modifier.fillMaxWidth().panel()) {
        // Brand strip; the panel's clip rounds its top corners
        Box(Modifier.fillMaxWidth().height(3.dp).background(CbColors.Red))
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BrandLabel(stringResource(R.string.current_mission))
            Text(mission.name, style = CbText.MissionName, color = CbColors.Text)
            Text(timeLeftLabel(mission.estimatedEnd, now), style = CbText.Countdown, color = CbColors.Amber)
            MissionDetails(mission.historicalStart, mission.weather)
            mission.next?.let { next ->
                HorizontalDivider(Modifier.padding(vertical = 8.dp), color = CbColors.PanelBorder)
                val start = DateFormat.getTimeFormat(context).format(Date.from(next.expectedStart))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandLabel(stringResource(R.string.next_label, start), Modifier.weight(1f))
                    // Offered while a reminder can still come before the start; once set, it stays to cancel
                    if (onToggleReminder != null && (reminderOn || canRemind(next.expectedStart, now))) {
                        ReminderBell(reminderOn, next.name, onToggleReminder)
                    }
                }
                Text(next.name, style = CbText.NextName, color = CbColors.Text)
                MissionDetails(next.historicalStart, next.weather)
            }
        }
    }
}

@Composable
private fun ReminderBell(on: Boolean, missionName: String, onToggle: () -> Unit) {
    // Pulled into the label's line height, so the bell doesn't make the card taller
    IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
        Icon(
            imageVector = if (on) Icons.Filled.Notifications else Icons.Outlined.Notifications,
            contentDescription = stringResource(if (on) R.string.reminder_cancel_cd else R.string.reminder_set_cd, missionName),
            tint = if (on) CbColors.Amber else CbColors.Muted,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MissionDetails(historicalStart: LocalDateTime?, weather: Weather?) {
    historicalStart?.let {
        Text(stringResource(R.string.in_game, inGameLabel(it)), style = MaterialTheme.typography.bodySmall, color = CbColors.Muted)
    }
    weather?.let {
        Text(keepPartsTogether(weatherLine(it)), style = MaterialTheme.typography.bodySmall, color = CbColors.Muted)
    }
}
