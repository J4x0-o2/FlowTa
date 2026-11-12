package com.j4x.flowta.core.presentation.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class CategoryColorsTest {

    private fun ids(n: Int) = (1..n).map { "id-$it" }

    private fun distance(a: Color, b: Color): Double {
        val dr = (a.red - b.red) * 255.0
        val dg = (a.green - b.green) * 255.0
        val db = (a.blue - b.blue) * 255.0
        return sqrt(dr * dr + dg * dg + db * db)
    }

    private fun minPairwiseDistance(palette: List<Color>): Double =
        palette.indices.flatMap { i -> (i + 1 until palette.size).map { j -> distance(palette[i], palette[j]) } }.min()

    @Test
    fun noCollisions_forAnyCountUpToPaletteSize() {
        for (n in 0..categoryPalette.size) {
            val colors = assignColors(ids(n), categoryPalette)
            assertEquals(n, colors.size)
            assertEquals("collision with $n ids", n, colors.values.toSet().size)
        }
    }

    @Test
    fun assignmentFollowsTheGivenOrder() {
        val colors = assignColors(listOf("b", "a", "c"), categoryPalette)
        assertEquals(categoryPalette[0], colors["b"])
        assertEquals(categoryPalette[1], colors["a"])
        assertEquals(categoryPalette[2], colors["c"])
    }

    @Test
    fun sameOrderedIdsAlwaysGiveTheSameColors() {
        assertEquals(assignColors(ids(8), categoryPalette), assignColors(ids(8), categoryPalette))
    }

    @Test
    fun addingACategoryAtTheEnd_keepsExistingColors() {
        val before = assignColors(ids(5), categoryPalette)
        val after = assignColors(ids(6), categoryPalette)
        assertTrue(before.all { (id, color) -> after[id] == color })
    }

    @Test
    fun wrapsAroundBeyondPaletteSize() {
        val size = categoryPalette.size
        val colors = assignColors(ids(size + 3), categoryPalette)
        assertEquals(size + 3, colors.size)
        assertEquals(colors["id-1"], colors["id-${size + 1}"])
        assertEquals(colors["id-3"], colors["id-${size + 3}"])
        assertNotEquals(colors["id-1"], colors["id-2"])
    }

    @Test
    fun duplicatedIds_keepTheirFirstSlot() {
        val colors = assignColors(listOf("a", "b", "a"), categoryPalette)
        assertEquals(categoryPalette[0], colors["a"])
        assertEquals(categoryPalette[1], colors["b"])
    }

    @Test
    fun categoryPaletteIsDistinctAndPerceptuallySeparated() {
        assertEquals(12, categoryPalette.size)
        assertEquals(categoryPalette.size, categoryPalette.toSet().size)
        assertTrue(minPairwiseDistance(categoryPalette) >= 65.0)
    }

    @Test
    fun incomeSourcePaletteIsDistinctAndPerceptuallySeparated() {
        assertEquals(incomeSourcePalette.size, incomeSourcePalette.toSet().size)
        assertTrue(minPairwiseDistance(incomeSourcePalette) >= 65.0)
        assertTrue(NoSourceColor !in incomeSourcePalette)
    }

    @Test
    fun metricsColors_categoriesAreDistinctAndStable() {
        val order = ids(10)
        val colors = metricsColors(expenseIds = order, sourceIds = emptyList())
        assertEquals(10, order.map { colors.category(it) }.toSet().size)
        assertEquals(colors.category("id-4"), metricsColors(order, emptyList()).category("id-4"))
    }

    @Test
    fun metricsColors_incomeSourcesUseTheirPaletteAndNullIsGray() {
        val sources = listOf("job", "gift", "sale")
        val colors = metricsColors(expenseIds = emptyList(), sourceIds = sources)
        assertEquals(sources.size, sources.map { colors.incomeSource(it) }.toSet().size)
        assertTrue(sources.all { colors.incomeSource(it) in incomeSourcePalette })
        assertEquals(NoSourceColor, colors.incomeSource(null))
    }

    @Test
    fun metricsColors_unknownIdsFallBackToNeutralGray() {
        val colors = metricsColors(listOf("a"), listOf("s"))
        assertEquals(NoSourceColor, colors.category("deleted"))
        assertEquals(NoSourceColor, colors.incomeSource("deleted"))
    }
}
