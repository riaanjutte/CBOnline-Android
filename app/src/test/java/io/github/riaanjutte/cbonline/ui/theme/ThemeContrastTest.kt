package io.github.riaanjutte.cbonline.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

/** WCAG AA for normal-size text: primary-coloured text (TextButtons, links) must reach 4.5:1. */
class ThemeContrastTest {

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05f) / (lo + 0.05f)
    }

    private fun assertLegible(label: String, fg: Color, bg: Color) {
        val ratio = contrast(fg, bg)
        assertTrue("$label contrast %.2f < 4.5".format(ratio), ratio >= 4.5f)
    }

    @Test
    fun `dark theme primary text is legible where the app uses it`() {
        assertLegible("primary on surface", DarkColors.primary, DarkColors.surface)
        assertLegible("primary on dialog (surfaceContainerHigh)", DarkColors.primary, DarkColors.surfaceContainerHigh)
        assertLegible("primary on update banner (secondaryContainer)", DarkColors.primary, DarkColors.secondaryContainer)
        assertLegible("button text (onPrimary) on primary", DarkColors.onPrimary, DarkColors.primary)
    }

    @Test
    fun `light theme primary text is legible on surface`() {
        assertLegible("primary on surface", LightColors.primary, LightColors.surface)
        assertLegible("button text (onPrimary) on primary", LightColors.onPrimary, LightColors.primary)
    }
}
