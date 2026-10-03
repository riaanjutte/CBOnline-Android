package io.github.riaanjutte.cbonline.ui

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.data.MissionInfo
import io.github.riaanjutte.cbonline.roster.Roster
import io.github.riaanjutte.cbonline.roster.RosterRow
import io.github.riaanjutte.cbonline.ui.brand.BrandHeader
import io.github.riaanjutte.cbonline.ui.brand.MapBackground
import io.github.riaanjutte.cbonline.ui.theme.StarColor
import io.github.riaanjutte.cbonline.ui.theme.color
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    state: RosterUiState,
    versionName: String,
    onRefresh: () -> Unit,
    onToggleFriend: (String) -> Unit,
    onDismissUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        MapBackground()
        Column(Modifier.fillMaxSize()) {
            BrandHeader(versionName, onOpenUrl)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
            val roster = state.roster
            if (roster != null) {
                RosterList(roster, state, onToggleFriend, onDismissUpdate, onOpenUrl)
            } else {
                // No roster yet, loading or failed: the mission comes from a different source, so keep it
                // on screen — including during each retry, which would otherwise flash a bare spinner
                Column(Modifier.fillMaxSize()) {
                    if (state.mission != null) MissionCard(state.mission, Modifier.padding(16.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (state.isLoading) CircularProgressIndicator() else LoadError(state.errorMessage, onRefresh)
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun LoadError(detail: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.load_failed), style = MaterialTheme.typography.titleMedium)
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun RosterList(
    roster: Roster,
    state: RosterUiState,
    onToggleFriend: (String) -> Unit,
    onDismissUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val updatedTime = state.lastUpdated?.let { DateFormat.getTimeFormat(context).format(Date.from(it)) }
    // LazyColumn's builder isn't composable, so resolve strings and colours here
    val friendsTitle = stringResource(R.string.section_friends)
    val axisTitle = stringResource(R.string.section_axis, roster.axisCount)
    val alliedTitle = stringResource(R.string.section_allied, roster.alliedCount)
    val unassignedTitle = stringResource(R.string.section_unassigned, roster.unassignedCount)
    val staleText = updatedTime?.let { stringResource(R.string.refresh_failed, it) }
    val nobodyText = stringResource(R.string.nobody_flying)

    // Banner and stale strip sit above the list, not in it: items inserted above a keyed
    // LazyColumn's first visible item land off-screen, and both arrive after the list is shown
    Column(Modifier.fillMaxSize()) {
        state.update?.let { info ->
            UpdateBanner(info, onDownload = { onOpenUrl(info.url) }, onDismiss = onDismissUpdate)
        }
        if (state.refreshFailed && staleText != null) {
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Text(staleText, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(16.dp, 8.dp))
            }
        }
        RosterItems(roster, state.mission, updatedTime, friendsTitle, axisTitle, alliedTitle, unassignedTitle, nobodyText, onToggleFriend)
    }
}

@Composable
private fun ColumnScope.RosterItems(
    roster: Roster,
    mission: MissionInfo?,
    updatedTime: String?,
    friendsTitle: String,
    axisTitle: String,
    alliedTitle: String,
    unassignedTitle: String,
    nobodyText: String,
    onToggleFriend: (String) -> Unit
) {
    // Edge-to-edge without a Scaffold: keep the last rows clear of the navigation bar
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp + navBarBottom)) {
        // Always the first item, even without data, so it's never inserted above the visible list
        item(key = "mission") { MissionCard(mission, Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)) }
        item(key = "summary") { SummaryCard(roster, updatedTime, Modifier.padding(16.dp)) }
        if (roster.total == 0) {
            item(key = "nobody") {
                Text(
                    nobodyText,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }
        }
        if (roster.starredCount > 0) {
            stickyHeader(key = "header:friends") { SectionHeader(friendsTitle, StarColor) }
            itemsIndexed(roster.friendsOnline, key = { i, r -> "friends:$i:${r.nickname}" }) { _, row ->
                PlayerRow(row, showSideTag = true, onToggle = { onToggleFriend(row.nickname) })
            }
            itemsIndexed(roster.friendsOffline, key = { i, name -> "friends-offline:$i:$name" }) { _, name ->
                OfflineFriendRow(name, onToggle = { onToggleFriend(name) })
            }
        }
        sideSection("axis", axisTitle, Coalition.Axis.color(), roster.axis, onToggleFriend)
        sideSection("allied", alliedTitle, Coalition.Allied.color(), roster.allied, onToggleFriend)
        if (roster.unassigned.isNotEmpty()) {
            sideSection("unassigned", unassignedTitle, Coalition.Unassigned.color(), roster.unassigned, onToggleFriend)
        }
    }
}

private fun LazyListScope.sideSection(
    section: String,
    title: String,
    accent: Color,
    rows: List<RosterRow>,
    onToggleFriend: (String) -> Unit
) {
    stickyHeader(key = "header:$section") { SectionHeader(title, accent) }
    // Keys include section and index: the API can list the same nickname twice
    itemsIndexed(rows, key = { i, r -> "$section:$i:${r.nickname}" }) { _, row ->
        PlayerRow(row, showSideTag = false, onToggle = { onToggleFriend(row.nickname) })
    }
}
