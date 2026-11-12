package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.core.domain.model.CurrencyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MetricsCalculationsTest {

    private fun cat(name: String, total: Double) = CategoryExpense(name, name, "EXPENSE", total)

    @Test
    fun donut_dropsNonPositiveCategoriesAndSumsTotal() {
        val data = computeDonutData(listOf(cat("a", 30.0), cat("b", 0.0), cat("c", 70.0), cat("d", -5.0)))
        assertEquals(listOf("a", "c"), data.slices.map { it.typeName })
        assertEquals(100.0, data.total, 0.0)
    }

    @Test
    fun donut_emptyOrAllZeroHasZeroTotal() {
        assertEquals(0.0, computeDonutData(emptyList()).total, 0.0)
        assertEquals(0.0, computeDonutData(listOf(cat("a", 0.0))).total, 0.0)
    }

    // --- savings ring ---

    @Test
    fun ring_fillsWithTheSavingsPercent() {
        val ring = savingsRing(income = 1000.0, expense = 250.0)
        assertEquals(0.75f, ring.progress, 0.0001f)
        assertEquals("75%", ring.label)
        assertFalse(ring.isNegative)
    }

    @Test
    fun ring_withoutIncomeIsEmptyWithADash() {
        val ring = savingsRing(income = 0.0, expense = 50.0)
        assertEquals(0f, ring.progress, 0f)
        assertEquals("—", ring.label)
        assertFalse(ring.isNegative)
    }

    @Test
    fun ring_whenOverspentIsEmptyAndNegative() {
        val ring = savingsRing(income = 100.0, expense = 150.0)
        assertEquals(0f, ring.progress, 0f)
        assertEquals("-50%", ring.label)
        assertTrue(ring.isNegative)
    }

    @Test
    fun ring_savingEverythingIsFull_andNothingSavedIsEmptyButNotNegative() {
        assertEquals(1f, savingsRing(100.0, 0.0).progress, 0f)
        val none = savingsRing(100.0, 100.0)
        assertEquals(0f, none.progress, 0f)
        assertEquals("0%", none.label)
        assertFalse(none.isNegative)
    }

    // --- compact money ---

    @Test
    fun compactMoney_keepsSmallAmountsFull() {
        assertEquals("$950", formatCompactMoney(950.0, "$", CurrencyCode.COP))
        assertEquals("$9,999", formatCompactMoney(9_999.0, "$", CurrencyCode.COP))
    }

    @Test
    fun compactMoney_shortensThousandsAndMillions() {
        assertEquals("$450 mil", formatCompactMoney(450_000.0, "$", CurrencyCode.COP))
        assertEquals("$12,5 mil", formatCompactMoney(12_500.0, "$", CurrencyCode.COP))
        assertEquals("$1,18 M", formatCompactMoney(1_180_000.0, "$", CurrencyCode.COP))
        assertEquals("$2 M", formatCompactMoney(2_000_000.0, "$", CurrencyCode.COP))
    }
}
