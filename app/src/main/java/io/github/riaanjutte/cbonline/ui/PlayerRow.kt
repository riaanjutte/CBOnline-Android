package io.github.riaanjutte.cbonline.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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

/** [modifier] carries the row's slice of its section panel (see `panelSegment`). */
@Composable
fun PlayerRow(row: RosterRow, showSideTag: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.nickname,
            style = MaterialTheme.typography.bodyMedium,
            color = CbColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showSideTag) {
            Spacer(Modifier.width(8.dp))
            Text(text = row.coalition.label().uppercase(Locale.ROOT), color = row.coalition.textColor(), style = CbText.Label)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = row.timeLabel, style = MaterialTheme.typography.bodyMedium, color = CbColors.Muted)
        StarButton(starred = row.isFriend, nickname = row.nickname, onToggle = onToggle)
    }
}

/** A starred friend who isn't online; still unstarrable so stale names can be removed. */
@Composable
fun OfflineFriendRow(nickname: String, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nickname,
            style = MaterialTheme.typography.bodyMedium,
            color = CbColors.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).alpha(0.5f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.offline),
            style = MaterialTheme.typography.bodyMedium,
            color = CbColors.Muted,
            modifier = Modifier.alpha(0.5f)
        )
        StarButton(starred = true, nickname = nickname, onToggle = onToggle)
    }
}

/** Sticky section header: opaque so rows scrolling under it don't show through, with a coloured edge. */
@Composable
fun SectionHeader(title: String, edge: Color, textColor: Color, position: SegmentPosition) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).panelSegment(position, opaque = true)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(edge))
        Text(
            text = title.uppercase(Locale.ROOT),
            style = CbText.SectionHeader,
            color = textColor,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun StarButton(starred: Boolean, nickname: String, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            painter = painterResource(if (starred) R.drawable.ic_star else R.drawable.ic_star_outline),
            contentDescription = stringResource(if (starred) R.string.unstar_cd else R.string.star_cd, nickname),
            tint = if (starred) StarColor else CbColors.StarOff
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
