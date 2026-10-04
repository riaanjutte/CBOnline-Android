package io.github.riaanjutte.cbonline.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.sp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.Coalition
import io.github.riaanjutte.cbonline.data.MissionInfo
import io.github.riaanjutte.cbonline.roster.Roster
import io.github.riaanjutte.cbonline.roster.RosterRow
import io.github.riaanjutte.cbonline.ui.brand.BrandHeader
import io.github.riaanjutte.cbonline.ui.brand.MapBackground
import io.github.riaanjutte.cbonline.ui.brand.panelSegment
import io.github.riaanjutte.cbonline.ui.brand.sectionPositions
import io.github.riaanjutte.cbonline.ui.theme.CbColors
import io.github.riaanjutte.cbonline.ui.theme.CbText
import io.github.riaanjutte.cbonline.ui.theme.color
import io.github.riaanjutte.cbonline.ui.theme.textColor
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RosterScreen(
    state: RosterUiState,
    versionName: String,
    onRefresh: () -> Unit,
    onToggleFriend: (String) -> Unit,
    onToggleSquad: (String) -> Unit,
    onOpenStats: (String) -> Unit,
    onDismissUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val actions = RowActions(onToggleFriend, onToggleSquad, onOpenStats)
    Box(Modifier.fillMaxSize()) {
        MapBackground()
        // Without a Scaffold, keep content clear of side navigation bars and cutouts in landscape;
        // the map stays full-bleed behind them
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))) {
            BrandHeader(versionName, onOpenUrl)
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                val roster = state.roster
                if (roster != null) {
                    RosterList(roster, state, actions, onDismissUpdate, onOpenUrl)
                } else {
                    // No roster yet, loading or failed: the mission comes from a different source, so keep it
                    // on screen — including during each retry, which would otherwise flash a bare spinner.
                    // A list, not a plain column, so pull-to-refresh works here too and nothing is cut off in landscape
                    LazyColumn(Modifier.fillMaxSize()) {
                        // Always present (an empty spacer until the mission arrives): an item inserted above the
                        // visible one would land off-screen, leaving the list scrolled past the mission card
                        item(key = "mission") { MissionCard(state.mission, Modifier.padding(10.dp)) }
                        item(key = "status") {
                            Box(Modifier.fillParentMaxHeight(0.7f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                if (state.isLoading) {
                                    CircularProgressIndicator(color = CbColors.Amber)
                                } else {
                                    LoadError(state.errorMessage, onRefresh)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** What a tap on a row can do. */
private class RowActions(
    val toggleFriend: (String) -> Unit,
    val toggleSquad: (String) -> Unit,
    val openStats: (String) -> Unit
)

@Composable
private fun LoadError(detail: String?, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(stringResource(R.string.load_failed).uppercase(Locale.ROOT), style = CbText.ErrorTitle, color = CbColors.Text)
        if (detail != null) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = CbColors.Muted, textAlign = TextAlign.Center)
        }
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = CbColors.Red, contentColor = Color.White)
        ) {
            Text(stringResource(R.string.retry).uppercase(Locale.ROOT), style = CbText.Button)
        }
    }
}

@Composable
private fun RosterList(
    roster: Roster,
    state: RosterUiState,
    actions: RowActions,
    onDismissUpdate: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val updatedTime = state.lastUpdated?.let { DateFormat.getTimeFormat(context).format(Date.from(it)) }
    // LazyColumn's builder isn't composable, so resolve strings here
    val titles = SectionTitles(
        friends = stringResource(R.string.section_friends),
        axis = stringResource(R.string.section_axis, roster.axisCount),
        allied = stringResource(R.string.section_allied, roster.alliedCount),
        unassigned = stringResource(R.string.section_unassigned, roster.unassignedCount)
    )
    val staleText = updatedTime?.let { stringResource(R.string.refresh_failed, it) }
    val nobodyText = stringResource(R.string.nobody_flying)

    // Banner and stale strip sit above the list, not in it: items inserted above a keyed
    // LazyColumn's first visible item land off-screen, and both arrive after the list is shown
    Column(Modifier.fillMaxSize()) {
        state.update?.let { info ->
            UpdateBanner(info, onDownload = { onOpenUrl(info.url) }, onDismiss = onDismissUpdate)
        }
        if (state.refreshFailed && staleText != null) {
            Box(Modifier.fillMaxWidth().background(CbColors.RedDeep)) {
                Text(staleText, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.padding(12.dp, 8.dp))
            }
        }
        RosterItems(roster, state.mission, updatedTime, titles, nobodyText, actions)
    }
}

private class SectionTitles(val friends: String, val axis: String, val allied: String, val unassigned: String)

@Composable
private fun ColumnScope.RosterItems(
    roster: Roster,
    mission: MissionInfo?,
    updatedTime: String?,
    titles: SectionTitles,
    nobodyText: String,
    actions: RowActions
) {
    // Edge-to-edge without a Scaffold: keep the last rows clear of the navigation bar
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 16.dp + navBarBottom)
    ) {
        // Always the first item, even without data, so it's never inserted above the visible list
        item(key = "mission") { MissionCard(mission, Modifier.padding(bottom = 10.dp)) }
        item(key = "summary") { SummaryCard(roster, updatedTime, Modifier.padding(bottom = 10.dp)) }
        if (roster.total == 0) {
            item(key = "nobody") {
                Text(
                    nobodyText.uppercase(Locale.ROOT),
                    style = CbText.Label.copy(fontSize = 14.sp),
                    color = CbColors.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )
            }
        }
        val online = roster.friendsOnline.size
        val offline = roster.friendsOffline.size
        val squadsOffline = roster.squadsOffline.size
        if (online + offline + squadsOffline > 0) {
            // Friends online, starred pilots offline and squads with nobody on form one panel under one header
            val pos = sectionPositions(online + offline + squadsOffline)
            stickyHeader(key = "header:friends") { SectionHeader(titles.friends, CbColors.Amber, CbColors.Amber, pos[0]) }
            itemsIndexed(roster.friendsOnline, key = { i, r -> "friends:$i:${r.nickname}" }) { i, row ->
                PlayerRow(
                    row, showSideTag = true,
                    onToggle = { actions.toggleFriend(row.nickname) }, onOpen = { actions.openStats(row.nickname) },
                    modifier = Modifier.panelSegment(pos[i + 1])
                )
            }
            itemsIndexed(roster.friendsOffline, key = { i, name -> "friends-offline:$i:$name" }) { i, name ->
                OfflineFriendRow(
                    name, onToggle = { actions.toggleFriend(name) }, onOpen = { actions.openStats(name) },
                    modifier = Modifier.panelSegment(pos[online + i + 1])
                )
            }
            itemsIndexed(roster.squadsOffline, key = { i, tag -> "squads-offline:$i:$tag" }) { i, tag ->
                OfflineSquadRow(tag, onUnstar = { actions.toggleSquad(tag) }, modifier = Modifier.panelSegment(pos[online + offline + i + 1]))
            }
            item(key = "gap:friends") { Spacer(Modifier.height(10.dp)) }
        }
        sideSection("axis", titles.axis, Coalition.Axis, roster.axis, actions)
        sideSection("allied", titles.allied, Coalition.Allied, roster.allied, actions)
        if (roster.unassigned.isNotEmpty()) {
            sideSection("unassigned", titles.unassigned, Coalition.Unassigned, roster.unassigned, actions)
        }
    }
}

private fun LazyListScope.sideSection(
    section: String,
    title: String,
    side: Coalition,
    rows: List<RosterRow>,
    actions: RowActions
) {
    val pos = sectionPositions(rows.size)
    stickyHeader(key = "header:$section") { SectionHeader(title, side.color(), side.textColor(), pos[0]) }
    // Keys include section and index: the API can list the same nickname twice
    itemsIndexed(rows, key = { i, r -> "$section:$i:${r.nickname}" }) { i, row ->
        PlayerRow(
            row, showSideTag = false,
            onToggle = { actions.toggleFriend(row.nickname) }, onOpen = { actions.openStats(row.nickname) },
            modifier = Modifier.panelSegment(pos[i + 1])
        )
    }
    item(key = "gap:$section") { Spacer(Modifier.height(10.dp)) }
}
