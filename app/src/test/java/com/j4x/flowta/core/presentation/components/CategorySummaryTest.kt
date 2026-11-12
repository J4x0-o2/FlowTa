package com.j4x.flowta.core.presentation.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CategorySummaryTest {

    @Test
    fun fewCategoriesAreJoinedWithDots() {
        assertEquals("Mercado · Café", listOf("Mercado", "Café").toCategorySummary())
        assertEquals("A · B · C", listOf("A", "B", "C").toCategorySummary())
    }

    @Test
    fun manyCategoriesCollapseIntoMoreCount() {
        assertEquals("A · B · +3 más", listOf("A", "B", "C", "D", "E").toCategorySummary())
    }
}
