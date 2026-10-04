package io.github.riaanjutte.cbonline.ui.brand

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.ui.AboutDialog
import io.github.riaanjutte.cbonline.ui.theme.CbColors

/** The wordmark banner's left and right edge colour. */
private val BANNER_EDGE = Color(0xFF424242)

/** Pinned Combat Box wordmark below the status bar, with the ⋮ menu (About) over its right end. */
@Composable
fun BrandHeader(
    versionName: String,
    onOpenUrl: (String) -> Unit,
    friendAlertsOn: Boolean = false,
    onToggleFriendAlerts: () -> Unit = {}
) {
    var menuOpen by remember { mutableStateOf(false) }
    var aboutOpen by rememberSaveable { mutableStateOf(false) }

    // Full width on a phone held upright (~43 dp tall). Wider screens would make it huge, so it stops at 56 dp
    // and sits centred on the banner's own edge colour, as on the website
    Box(Modifier.fillMaxWidth().statusBarsPadding().background(BANNER_EDGE)) {
        Image(
            painter = painterResource(R.drawable.cb_wordmark),
            contentDescription = "Combat Box",
            modifier = Modifier.fillMaxWidth().heightIn(max = 56.dp),
            contentScale = ContentScale.Fit
        )
        // matchParentSize: the banner (~43 dp on a phone) sets the height, not the 48 dp button
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options), tint = CbColors.Text)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    // An on/off item: a check shows when alerts are on, and TalkBack hears it as a checkbox
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.friend_alerts)) },
                        onClick = { menuOpen = false; onToggleFriendAlerts() },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Check, contentDescription = null,
                                tint = if (friendAlertsOn) CbColors.Amber else CbColors.Panel
                            )
                        },
                        modifier = Modifier.semantics { toggleableState = ToggleableState(friendAlertsOn) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.about)) },
                        onClick = { menuOpen = false; aboutOpen = true }
                    )
                }
            }
        }
    }

    if (aboutOpen) AboutDialog(versionName, onOpenUrl) { aboutOpen = false }
}
