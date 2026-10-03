package io.github.riaanjutte.cbonline.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.riaanjutte.cbonline.data.Coalition

private val Indigo = Color(0xFF4F5BD5)
// Lighter tone of the same indigo: #4F5BD5 text is illegible (<4.5:1) on dark surfaces
private val IndigoLight = Color(0xFFBAC3FF)
private val IndigoDeep = Color(0xFF1B247A)

val StarColor = Color(0xFFF2B705)

internal val LightColors = lightColorScheme(primary = Indigo, onPrimary = Color.White)
internal val DarkColors = darkColorScheme(primary = IndigoLight, onPrimary = IndigoDeep)

/** Fixed side colours (shared with the desktop CBOnline app), never wallpaper-derived. */
fun Coalition.color(): Color = when (this) {
    Coalition.Axis -> Color(0xFF2F80ED)
    Coalition.Allied -> Color(0xFFE5484D)
    Coalition.Unassigned -> Color(0xFF8B949E)
}

@Composable
fun CbOnlineTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
