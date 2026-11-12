package com.j4x.flowta.core.presentation.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryColorsTest {

    @Test
    fun sameIdAlwaysGivesTheSameColor() {
        assertEquals(categoryColor("food-123"), categoryColor("food-123"))
        assertEquals(categoryColorIndex("food-123"), categoryColorIndex("food-123"))
    }

    @Test
    fun indexStaysInsidePalette_forAnyId() {
        val ids = listOf("", "a", "transport", "ÁÉÍ", "Integer.MIN_VALUE:polygenelubricants", "x".repeat(500)) +
            (1..500).map { "id-$it" }
        assertTrue(ids.all { categoryColorIndex(it) in 0 until CATEGORY_PALETTE_SIZE })
    }

    @Test
    fun idsSpreadAcrossTheWholePalette() {
        val used = (1..500).map { categoryColorIndex("type-$it") }.toSet()
        assertEquals(CATEGORY_PALETTE_SIZE, used.size)
    }

    @Test
    fun paletteColorsAreAllDistinct() {
        val colors = (0 until CATEGORY_PALETTE_SIZE).map { categoryPalette[it] }
        assertEquals(CATEGORY_PALETTE_SIZE, colors.toSet().size)
    }

    @Test
    fun colorMatchesItsPaletteIndex() {
        assertEquals(categoryPalette[categoryColorIndex("abc")], categoryColor("abc"))
    }
}
