package io.github.riaanjutte.cbonline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.roster.RosterRow
import io.github.riaanjutte.cbonline.ui.brand.SegmentPosition
import io.github.riaanjutte.cbonline.ui.brand.panelSegment
import io.github.riaanjutte.cbonline.ui.theme.CbColors
import io.github.riaanjutte.cbonline.ui.theme.CbText
import io.github.riaanjutte.cbonline.ui.theme.StarColor
import io.github.riaanjutte.cbonline.ui.theme.textColor
import java.util.Locale

/**
 * Tapping the row opens the pilot's stats; the star is its own button. [modifier] carries the row's slice of
 * its section panel (see `panelSegment`).
 */
@Composable
fun PlayerRow(row: RosterRow, showSideTag: Boolean, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.show_stats), onClick = onOpen)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The star shows squad membership only as a colour, so TalkBack hears it with the name
        val squadNote = row.squad?.let { stringResource(R.string.in_squad_cd, row.nickname, it) }
        Text(
            text = row.nickname,
            style = MaterialTheme.typography.bodyMedium,
            color = CbColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(if (squadNote != null) Modifier.semantics { contentDescription = squadNote } else Modifier)
        )
        if (showSideTag) {
            Spacer(Modifier.width(8.dp))
            val side = row.coalition.label()
            Text(
                text = side.uppercase(Locale.ROOT),
                color = row.coalition.textColor(),
                style = CbText.Label,
                // Read as a word, not spelled out letter by letter
                modifier = Modifier.semantics { contentDescription = side }
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = row.timeLabel, style = MaterialTheme.typography.bodyMedium, color = CbColors.Muted)
        StarButton(
            starred = row.isFriend, viaSquad = row.squad != null,
            description = stringResource(R.string.star_cd, row.nickname), onToggle = onToggle
        )
    }
}

/** A starred friend who isn't online; still unstarrable so stale names can be removed, and still tappable for stats. */
@Composable
fun OfflineFriendRow(nickname: String, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = stringResource(R.string.show_stats), onClick = onOpen)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Muted rather than faded: half-transparent text fell below the 4.5:1 contrast minimum
        Text(
            text = nickname,
            style = MaterialTheme.typography.bodyMedium,
            color = CbColors.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.offline), style = MaterialTheme.typography.bodyMedium, color = CbColors.Muted)
        StarButton(starred = true, description = stringResource(R.string.star_cd, nickname), onToggle = onToggle)
    }
}

/** A starred squad with nobody online; unstarrable from here. */
@Composable
fun OfflineSquadRow(tag: String, onUnstar: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tag,
            style = MaterialTheme.typography.bodyMedium,
            color = CbColors.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.squad_offline), style = MaterialTheme.typography.bodyMedium, color = CbColors.Muted)
        StarButton(starred = true, description = stringResource(R.string.squad_star_tag, tag), onToggle = onUnstar)
    }
}

/** Sticky section header: opaque so rows scrolling under it don't show through, with a coloured edge. */
@Composable
fun SectionHeader(title: String, edge: Color, textColor: Color, position: SegmentPosition) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .panelSegment(position, opaque = true)
            // Merged so TalkBack focuses the header itself, which carries the heading flag
            .semantics(mergeDescendants = true) { heading() }
    ) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(edge))
        Text(
            text = title.uppercase(Locale.ROOT),
            style = CbText.SectionHeader,
            color = textColor,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
        )
    }
}

/**
 * An on/off switch, so TalkBack reads its [description] ("Star Hans") with whether it's on. Filled when starred;
 * an amber outline when only the pilot's squad is starred ([viaSquad]); tapping that stars the pilot as well.
 */
@Composable
private fun StarButton(starred: Boolean, description: String, onToggle: () -> Unit, viaSquad: Boolean = false) {
    IconToggleButton(checked = starred, onCheckedChange = { onToggle() }) {
        Icon(
            painter = painterResource(if (starred) R.drawable.ic_star else R.drawable.ic_star_outline),
            contentDescription = description,
            tint = if (starred || viaSquad) StarColor else CbColors.StarOff
        )
    }
}

@Composable
fun Coalition.label(): String = stringResource(
    when (this) {
        Coalition.Axis -> R.string.side_axis
        Coalition.Allied -> R.string.side_allied
        Coalition.Unassigned -> R.string.side_unassigned
    }
)
