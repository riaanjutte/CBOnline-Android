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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.roster.Roster
import io.github.riaanjutte.cbonline.ui.brand.BrandLabel
import io.github.riaanjutte.cbonline.ui.brand.panel
import io.github.riaanjutte.cbonline.ui.theme.CbColors
import io.github.riaanjutte.cbonline.ui.theme.CbText
import io.github.riaanjutte.cbonline.ui.theme.color
import io.github.riaanjutte.cbonline.ui.theme.textColor
import java.util.Locale

@Composable
fun SummaryCard(roster: Roster, updatedTime: String?, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().panel().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${roster.total}", style = CbText.BigNumber, color = CbColors.Text)
            BrandLabel(stringResource(R.string.pilots_online), Modifier.padding(start = 8.dp, bottom = 6.dp))
        }
        if (roster.total > 0) BalanceBar(roster)
        SidesLine(roster)
        val friends = if (roster.starredCount > 0) {
            stringResource(R.string.friends_online, roster.friendsOnlineCount, roster.starredCount)
        } else {
            null
        }
        val updated = updatedTime?.let { stringResource(R.string.updated_at, it) }
        listOfNotNull(friends, updated).takeIf { it.isNotEmpty() }?.let {
            Text(it.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = CbColors.Muted)
        }
    }
}

/** "14 AXIS · 9 ALLIED", each side in its own text colour (built from the per-side labels so it can be coloured). */
@Composable
private fun SidesLine(roster: Roster) {
    val parts = buildList {
        add(Triple(roster.axisCount, stringResource(R.string.side_axis), Coalition.Axis))
        add(Triple(roster.alliedCount, stringResource(R.string.side_allied), Coalition.Allied))
        if (roster.unassignedCount > 0) add(Triple(roster.unassignedCount, stringResource(R.string.side_unassigned), Coalition.Unassigned))
    }
    val text = buildAnnotatedString {
        parts.forEachIndexed { i, (count, label, side) ->
            if (i > 0) withStyle(SpanStyle(color = CbColors.Muted)) { append(" · ") }
            withStyle(SpanStyle(color = side.textColor())) { append("$count ${label.uppercase(Locale.ROOT)}") }
        }
    }
    Text(text, style = CbText.SectionHeader)
}

/** Side balance, Axis · Allied · Unassigned, proportional to player counts. */
@Composable
private fun BalanceBar(roster: Roster) {
    Row(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
        listOf(
            Coalition.Axis to roster.axisCount,
            Coalition.Allied to roster.alliedCount,
            Coalition.Unassigned to roster.unassignedCount
        ).filter { it.second > 0 }.forEach { (side, count) ->
            Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(side.color()))
        }
    }
}
