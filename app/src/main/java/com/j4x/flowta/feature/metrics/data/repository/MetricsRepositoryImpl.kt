package com.j4x.flowta.feature.metrics.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.j4x.flowta.database.TransactionQueries
import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.DateRange
import com.j4x.flowta.feature.metrics.domain.model.IncomeSourceTotal
import com.j4x.flowta.feature.metrics.domain.repository.MetricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MetricsRepositoryImpl @Inject constructor(
    private val queries: TransactionQueries
) : MetricsRepository {

    override fun getExpensesByCategory(range: DateRange): Flow<List<CategoryExpense>> =
        queries.totalByCategoryInRange(range.from, range.to)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list ->
                list.map { row ->
                    CategoryExpense(row.type_id, row.type_name, row.category, row.total)
                }
            }

    override fun getIncomeBySource(range: DateRange): Flow<List<IncomeSourceTotal>> =
        queries.incomeBySourceInRange(range.from, range.to)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list -> list.map { IncomeSourceTotal(it.source_id, it.source_name, it.total) } }
}
