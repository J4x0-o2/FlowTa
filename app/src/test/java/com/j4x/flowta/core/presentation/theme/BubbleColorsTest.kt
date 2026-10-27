package com.j4x.flowta.core.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleColorsTest {

    private fun contrast(a: Color, b: Color): Float {
        val (hi, lo) = maxOf(a.luminance(), b.luminance()) to minOf(a.luminance(), b.luminance())
        return (hi + 0.05f) / (lo + 0.05f)
    }

    @Test
    fun picksColorsPerFlowTypeAndTheme() {
        assertEquals(IncomeBubbleLight, bubbleColors("INCOME", dark = false).container)
        assertEquals(IncomeBubbleDark, bubbleColors("INCOME", dark = true).container)
        assertEquals(TransferBubbleDark, bubbleColors("TRANSFER", dark = true).container)
        assertEquals(ExpenseBubbleLight, bubbleColors("EXPENSE", dark = false).container)
    }

    @Test
    fun amountAndDateMeetWcagAaInAllCombinations() {
        for (flow in listOf("INCOME", "EXPENSE", "TRANSFER")) for (dark in listOf(false, true)) {
            val c = bubbleColors(flow, dark)
            assertTrue("$flow dark=$dark amount", contrast(c.container, c.content) >= 4.5f)
            assertTrue("$flow dark=$dark date", contrast(c.container, c.date) >= 4.5f)
        }
    }
}
