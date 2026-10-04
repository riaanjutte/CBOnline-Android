package io.github.riaanjutte.cbonline.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.riaanjutte.cbonline.R
import io.github.riaanjutte.cbonline.data.Coalition

/** Combat Box brand tokens, from the CB stats-site rebrand (CombatBoxArt/theme/combat-box.css). */
object CbColors {
    val Background = Color(0xFF161817)
    val Panel = Color(0xFF1C1F1E)
    val PanelTranslucent = Panel.copy(alpha = 0.94f)
    val PanelBorder = Color.White.copy(alpha = 0.11f)
    val Text = Color(0xFFF3F5F2)
    val Muted = Color(0xFFAEB7B3)
    /** Brand red, a shade deeper than the stats site's #DF392D so white button text reaches 4.5:1. */
    val Red = Color(0xFFD2352A)
    val RedDeep = Color(0xFFA91F18)
    val Amber = Color(0xFFD4AA5D)
    val Sky = Color(0xFF7BC4EE)
    val Axis = Color(0xFF3D9ED8)
    val Allied = Color(0xFFD14F43)
    /** Brand Allied red is only ~3.9:1 as small text on a panel; this lighter red is used for text. */
    val AlliedText = Color(0xFFE8695E)
    val Unassigned = Color(0xFF8B949E)
    val StarOff = Color(0xFF6D7471)
}

val StarColor: Color = CbColors.Amber

/** Fill colour for bars and section edges. */
fun Coalition.color(): Color = when (this) {
    Coalition.Axis -> CbColors.Axis
    Coalition.Allied -> CbColors.Allied
    Coalition.Unassigned -> CbColors.Unassigned
}

/** Colour for side names and tags shown as text. */
fun Coalition.textColor(): Color = when (this) {
    Coalition.Allied -> CbColors.AlliedText
    else -> color()
}

internal val CbDarkScheme: ColorScheme = darkColorScheme(
    primary = CbColors.Red,
    onPrimary = Color.White,
    secondary = CbColors.Amber,
    onSecondary = CbColors.Background,
    tertiary = CbColors.Sky,
    background = CbColors.Background,
    onBackground = CbColors.Text,
    surface = CbColors.Panel,
    onSurface = CbColors.Text,
    surfaceVariant = CbColors.Panel,
    onSurfaceVariant = CbColors.Muted,
    // Menus, bottom sheets and dialogs each pick a different container; all of them are the brand panel,
    // and the panel-coloured tint makes elevation tinting a no-op
    surfaceContainerLowest = CbColors.Panel,
    surfaceContainerLow = CbColors.Panel,
    surfaceContainer = CbColors.Panel,
    surfaceContainerHigh = CbColors.Panel,
    surfaceContainerHighest = CbColors.Panel,
    surfaceBright = CbColors.Panel,
    surfaceDim = CbColors.Panel,
    surfaceTint = CbColors.Panel,
    secondaryContainer = CbColors.Panel,
    onSecondaryContainer = CbColors.Text,
    outline = CbColors.PanelBorder,
    error = CbColors.RedDeep,
    onError = Color.White
)

/** Oswald is one variable font file; each weight is a variation of it. */
@OptIn(ExperimentalTextApi::class)
val Oswald = FontFamily(
    Font(R.font.oswald, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.oswald, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.oswald, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600)))
)

private val Base = Typography()

/** Oswald for display, headline, title and label styles; body text keeps the system font. */
val CbTypography = Base.copy(
    displayLarge = Base.displayLarge.copy(fontFamily = Oswald),
    displayMedium = Base.displayMedium.copy(fontFamily = Oswald),
    displaySmall = Base.displaySmall.copy(fontFamily = Oswald),
    headlineLarge = Base.headlineLarge.copy(fontFamily = Oswald),
    headlineMedium = Base.headlineMedium.copy(fontFamily = Oswald),
    headlineSmall = Base.headlineSmall.copy(fontFamily = Oswald),
    titleLarge = Base.titleLarge.copy(fontFamily = Oswald),
    titleMedium = Base.titleMedium.copy(fontFamily = Oswald),
    titleSmall = Base.titleSmall.copy(fontFamily = Oswald),
    labelLarge = Base.labelLarge.copy(fontFamily = Oswald),
    labelMedium = Base.labelMedium.copy(fontFamily = Oswald),
    labelSmall = Base.labelSmall.copy(fontFamily = Oswald)
)

/** Named brand text styles (spec §3 typography table). */
object CbText {
    val MissionName = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 19.sp)
    val Countdown = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, lineHeight = 30.sp)
    val BigNumber = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.SemiBold, fontSize = 40.sp, lineHeight = 44.sp)
    val Label = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.08.em)
    val SectionHeader = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.08.em)
    val NextName = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Normal, fontSize = 15.sp)
    val ErrorTitle = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 18.sp)
    val Button = TextStyle(fontFamily = Oswald, fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.08.em)
}

/** Always the dark brand look, whatever the phone's light/dark setting. */
@Composable
fun CbOnlineTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CbDarkScheme, typography = CbTypography, content = content)
}
