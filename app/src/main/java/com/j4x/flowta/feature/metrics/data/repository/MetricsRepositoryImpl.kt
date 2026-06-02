package com.j4x.flowta.feature.metrics.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.j4x.flowta.database.InternalTransferQueries
import com.j4x.flowta.database.TransactionQueries
import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.DailyFlow
import com.j4x.flowta.feature.metrics.domain.model.DateRange
import com.j4x.flowta.feature.metrics.domain.repository.MetricsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class MetricsRepositoryImpl @Inject constructor(
    private val queries: TransactionQueries,
    private val transferQueries: InternalTransferQueries
) : MetricsRepository {

    override fun getExpensesByCategory(range: DateRange): Flow<List<CategoryExpense>> =
        queries.totalByCategoryInRange(range.from, range.to)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list ->
                list.map { row ->
                    CategoryExpense(row.type_id, row.type_name, row.category, row.total)
                }
            }

    override fun getIncomeVsExpenseByDay(range: DateRange): Flow<List<DailyFlow>> =
        queries.incomeVsExpenseByDay(range.from, range.to)
            .asFlow().mapToList(Dispatchers.IO)
            .map { list ->
                list.map { row -> DailyFlow(row.flow_type, row.total, row.day_bucket) }
            }

    override fun getNetBalance(): Flow<Double> =
        queries.netBalance().asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0.0 }

    override fun getTotalTransfers(range: DateRange): Flow<Double> =
        transferQueries.totalAmountInRange(range.from, range.to)
            .asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0.0 }
}
