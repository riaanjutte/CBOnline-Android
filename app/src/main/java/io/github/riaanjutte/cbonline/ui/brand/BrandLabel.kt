package io.github.riaanjutte.cbonline.ui.brand

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.riaanjutte.cbonline.ui.theme.CbColors
import io.github.riaanjutte.cbonline.ui.theme.CbText
import java.util.Locale

/** Small uppercase Oswald label ("CURRENT MISSION", "PILOTS ONLINE"); resources stay in sentence case. */
@Composable
fun BrandLabel(text: String, modifier: Modifier = Modifier, color: Color = CbColors.Muted) {
    Text(text.uppercase(Locale.ROOT), modifier = modifier, style = CbText.Label, color = color)
}
