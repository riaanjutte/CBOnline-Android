package io.github.riaanjutte.cbonline.ui.brand

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import io.github.riaanjutte.cbonline.ui.brand.SegmentPosition.First
import io.github.riaanjutte.cbonline.ui.brand.SegmentPosition.Last
import io.github.riaanjutte.cbonline.ui.brand.SegmentPosition.Middle
import io.github.riaanjutte.cbonline.ui.brand.SegmentPosition.Only
import org.junit.Assert.assertEquals
import org.junit.Test

class PanelTest {

    @Test
    fun `a header with no rows is a complete panel`() = assertEquals(listOf(Only), sectionPositions(0))

    @Test
    fun `a header with one row`() = assertEquals(listOf(First, Last), sectionPositions(1))

    @Test
    fun `positions span all rows`() =
        // e.g. Friends with 1 online + 2 offline: one continuous panel
        assertEquals(listOf(First, Middle, Middle, Last), sectionPositions(3))

    @Test
    fun `shapes round only the outer corners`() {
        assertEquals(RoundedCornerShape(8.dp), segmentShape(Only))
        assertEquals(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomEnd = 0.dp, bottomStart = 0.dp), segmentShape(First))
        assertEquals(RoundedCornerShape(0.dp), segmentShape(Middle))
        assertEquals(RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomEnd = 8.dp, bottomStart = 8.dp), segmentShape(Last))
    }
}
