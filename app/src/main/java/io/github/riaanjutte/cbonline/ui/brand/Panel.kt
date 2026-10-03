package io.github.riaanjutte.cbonline.ui.brand

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.ui.theme.CbColors

/** Where a list item sits in a section panel (a sticky header followed by its rows). */
enum class SegmentPosition { Only, First, Middle, Last }

/** Positions for a section header followed by [rowCount] rows, so the whole section reads as one panel. */
fun sectionPositions(rowCount: Int): List<SegmentPosition> =
    if (rowCount == 0) {
        listOf(SegmentPosition.Only)
    } else {
        List(rowCount + 1) { i ->
            when (i) {
                0 -> SegmentPosition.First
                rowCount -> SegmentPosition.Last
                else -> SegmentPosition.Middle
            }
        }
    }

private val Radius = 8.dp
private val PanelShape = RoundedCornerShape(Radius)

/** Rounds only the corners on the panel's outside edge. */
fun segmentShape(position: SegmentPosition): Shape = when (position) {
    SegmentPosition.Only -> RoundedCornerShape(Radius)
    SegmentPosition.First -> RoundedCornerShape(topStart = Radius, topEnd = Radius, bottomEnd = 0.dp, bottomStart = 0.dp)
    SegmentPosition.Middle -> RoundedCornerShape(0.dp)
    SegmentPosition.Last -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = Radius, bottomStart = Radius)
}

/** Card panel over the map: clipped, translucent, thin border. Clipping also trims children (e.g. top strips). */
fun Modifier.panel(): Modifier = this
    .clip(PanelShape)
    .background(CbColors.PanelTranslucent)
    .border(1.dp, CbColors.PanelBorder, PanelShape)

/**
 * One list item's slice of a section panel. No border: a border can't run continuously across
 * separate lazy-list items without seams. Sticky headers pass [opaque] so rows don't show through.
 */
fun Modifier.panelSegment(position: SegmentPosition, opaque: Boolean = false): Modifier = this
    .clip(segmentShape(position))
    .background(if (opaque) CbColors.Panel else CbColors.PanelTranslucent)
