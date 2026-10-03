package io.github.riaanjutte.cbonline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.roster.RosterRow
import io.github.riaanjutte.cbonline.ui.theme.StarColor
import io.github.riaanjutte.cbonline.ui.theme.color

@Composable
fun PlayerRow(row: RosterRow, showSideTag: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.nickname,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (showSideTag) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = row.coalition.label(),
                color = row.coalition.color(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = row.timeLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        StarButton(starred = row.isFriend, nickname = row.nickname, onToggle = onToggle)
    }
}

/** A starred friend who isn't online; still unstarrable so stale names can be removed. */
@Composable
fun OfflineFriendRow(nickname: String, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nickname,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).alpha(0.5f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(R.string.offline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(0.5f)
        )
        StarButton(starred = true, nickname = nickname, onToggle = onToggle)
    }
}

@Composable
fun SectionHeader(title: String, accent: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = accent,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun StarButton(starred: Boolean, nickname: String, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            painter = painterResource(if (starred) R.drawable.ic_star else R.drawable.ic_star_outline),
            contentDescription = stringResource(if (starred) R.string.unstar_cd else R.string.star_cd, nickname),
            tint = if (starred) StarColor else MaterialTheme.colorScheme.onSurfaceVariant
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
