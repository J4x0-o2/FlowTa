package com.j4x.flowta.feature.metrics.domain.repository

import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.DateRange
import kotlinx.coroutines.flow.Flow

interface MetricsRepository {
    fun getExpensesByCategory(range: DateRange): Flow<List<CategoryExpense>>
    fun getNetBalance(): Flow<Double>
    fun getTotalTransfers(range: DateRange): Flow<Double>
}
