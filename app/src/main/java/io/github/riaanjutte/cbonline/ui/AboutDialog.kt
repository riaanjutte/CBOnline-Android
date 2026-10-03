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

private const val SOURCE_URL = "https://github.com/riaanjutte/CBOnline-Android"

@Composable
fun AboutDialog(versionName: String, onOpenUrl: (String) -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.close)) } },
        icon = { Image(painterResource(R.drawable.cb_logo), contentDescription = null, modifier = Modifier.size(72.dp)) },
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.about_version, versionName))
                Text(stringResource(R.string.about_data))
                Text(
                    stringResource(R.string.about_source),
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onOpenUrl(SOURCE_URL) }
                )
            }
        }
    )
}
