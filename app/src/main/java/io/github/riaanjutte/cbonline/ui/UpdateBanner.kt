package io.github.riaanjutte.cbonline.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.UpdateInfo
import io.github.riaanjutte.cbonline.ui.theme.CbColors
import io.github.riaanjutte.cbonline.ui.theme.CbText
import java.util.Locale

@Composable
fun UpdateBanner(info: UpdateInfo, onDownload: () -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(CbColors.Panel)) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.update_available, info.version).uppercase(Locale.ROOT),
                style = CbText.SectionHeader,
                color = CbColors.Text,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDownload) {
                Text(stringResource(R.string.download).uppercase(Locale.ROOT), style = CbText.Button, color = CbColors.Amber)
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss).uppercase(Locale.ROOT), style = CbText.Button, color = CbColors.Muted)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(CbColors.Amber))
    }
}
