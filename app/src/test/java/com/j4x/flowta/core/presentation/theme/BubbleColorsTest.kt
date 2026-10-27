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

    @Test
    fun flowColorsMeetContrastOnAppSurfaces() {
        val light = listOf(LightBackground, LightSurface, LightSurface2) to false
        val dark = listOf(DarkBackground, DarkSurface, DarkSurface2) to true
        for ((surfaces, isDark) in listOf(light, dark)) {
            val f = flowColorsFor(isDark)
            for (s in surfaces) {
                assertTrue("incomeText dark=$isDark", contrast(s, f.incomeText) >= 4.5f)
                assertTrue("expenseText dark=$isDark", contrast(s, f.expenseText) >= 4.5f)
                assertTrue("incomeFill dark=$isDark", contrast(s, f.incomeFill) >= 3f)
                assertTrue("expenseFill dark=$isDark", contrast(s, f.expenseFill) >= 3f)
            }
            assertTrue("incomeSolid label", contrast(f.incomeSolid, f.onSolid) >= 4.5f)
            assertTrue("expenseSolid label", contrast(f.expenseSolid, f.onSolid) >= 4.5f)
        }
    }
}
