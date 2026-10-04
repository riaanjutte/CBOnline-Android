package io.github.riaanjutte.cbonline.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * WCAG AA for normal-size text: every brand colour used as text must reach 4.5:1 on what it sits on.
 * Panels over the map are 94 % opaque and the map is darker than the panel, so testing against the
 * opaque panel colour is the worst case.
 */
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
    fun `brand text colours are legible on panels`() {
        mapOf(
            "Text" to CbColors.Text, "Muted" to CbColors.Muted, "Amber" to CbColors.Amber, "Axis" to CbColors.Axis,
            "AlliedText" to CbColors.AlliedText, "Unassigned" to CbColors.Unassigned, "Sky" to CbColors.Sky
        ).forEach { (name, c) -> assertLegible("$name on Panel", c, CbColors.Panel) }
    }

    @Test
    fun `text on brand fills is legible`() {
        assertLegible("white on Red (Retry)", Color.White, CbColors.Red)
        assertLegible("white on RedDeep (stale strip)", Color.White, CbColors.RedDeep)
    }

    @Test
    fun `scheme text roles are legible`() {
        with(CbDarkScheme) {
            assertLegible("onSurface", onSurface, surface)
            assertLegible("onSurfaceVariant", onSurfaceVariant, surface)
            assertLegible("secondary text buttons", secondary, surface)
            assertLegible("tertiary links", tertiary, surface)
            assertLegible("onPrimary", onPrimary, primary)
            assertLegible("onError", onError, error)
        }
    }

    @Test
    fun `menus, sheets and dialogs use the brand panel, not Material's purple defaults`() {
        with(CbDarkScheme) {
            mapOf(
                "surfaceContainerLowest" to surfaceContainerLowest, "surfaceContainerLow" to surfaceContainerLow,
                "surfaceContainer" to surfaceContainer, "surfaceContainerHigh" to surfaceContainerHigh,
                "surfaceContainerHighest" to surfaceContainerHighest, "surfaceBright" to surfaceBright, "surfaceDim" to surfaceDim
            ).forEach { (name, c) -> assertEquals(name, CbColors.Panel, c) }
            // Elevated surfaces are tinted with surfaceTint; the panel colour makes that a no-op
            assertEquals("surfaceTint", CbColors.Panel, surfaceTint)
        }
    }
}
