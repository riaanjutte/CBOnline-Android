package io.github.riaanjutte.cbonline.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.ui.theme.CbColors
import io.github.riaanjutte.cbonline.ui.theme.CbText
import java.util.Locale

private const val SOURCE_URL = "https://github.com/riaanjutte/CBOnline-Android"

@Composable
fun AboutDialog(versionName: String, onOpenUrl: (String) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        containerColor = CbColors.Panel,
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.close).uppercase(Locale.ROOT), style = CbText.Button, color = CbColors.Amber)
            }
        },
        icon = { Image(painterResource(R.drawable.cb_logo), contentDescription = null, modifier = Modifier.size(68.dp)) },
        title = { Text(stringResource(R.string.app_name).uppercase(Locale.ROOT), style = CbText.MissionName, color = CbColors.Text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.about_version, versionName), style = MaterialTheme.typography.bodyMedium, color = CbColors.Muted)
                Text(
                    stringResource(R.string.about_source),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CbColors.Sky,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onOpenUrl(SOURCE_URL) }
                )
            }
        }
    )
}
