package com.j4x.flowta.feature.metrics.domain.repository

import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.DateRange
import com.j4x.flowta.feature.metrics.domain.model.IncomeSourceTotal
import kotlinx.coroutines.flow.Flow

interface MetricsRepository {
    fun getExpensesByCategory(range: DateRange): Flow<List<CategoryExpense>>
    /** Income summed by source in the half-open range [DateRange.from], [DateRange.to), biggest first. */
    fun getIncomeBySource(range: DateRange): Flow<List<IncomeSourceTotal>>
}
