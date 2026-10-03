package io.github.riaanjutte.cbonline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.roster.Roster
import io.github.riaanjutte.cbonline.ui.theme.color

@Composable
fun SummaryCard(roster: Roster, updatedTime: String?, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.online_count, roster.total), style = MaterialTheme.typography.displaySmall)
            if (roster.total > 0) BalanceBar(roster)
            Text(
                if (roster.unassignedCount > 0) {
                    stringResource(R.string.sides_unassigned, roster.axisCount, roster.alliedCount, roster.unassignedCount)
                } else {
                    stringResource(R.string.sides, roster.axisCount, roster.alliedCount)
                },
                style = MaterialTheme.typography.bodyLarge
            )
            if (roster.starredCount > 0) {
                Text(
                    stringResource(R.string.friends_online, roster.friendsOnline.size, roster.starredCount),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (updatedTime != null) {
                Text(
                    stringResource(R.string.updated_at, updatedTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Side balance, Axis · Allied · Unassigned, proportional to player counts. */
@Composable
private fun BalanceBar(roster: Roster) {
    Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
        listOf(
            Coalition.Axis to roster.axisCount,
            Coalition.Allied to roster.alliedCount,
            Coalition.Unassigned to roster.unassignedCount
        ).filter { it.second > 0 }.forEach { (side, count) ->
            Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(side.color()))
        }
    }
}
