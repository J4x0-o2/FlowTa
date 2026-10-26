package com.j4x.flowta.feature.history.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.j4x.flowta.database.TransactionQueries
import com.j4x.flowta.feature.history.domain.model.HistoryMovement
import com.j4x.flowta.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val transactionQueries: TransactionQueries
) : HistoryRepository {

    override fun observeMovements(fromMillis: Long, toMillis: Long): Flow<List<HistoryMovement>> =
        transactionQueries.selectMovementsInRange(fromMillis, toMillis)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { rows -> rows.map { HistoryMovement(it.flow_type, it.amount, it.created_at) } }

    override fun observeEarliest(): Flow<Long?> =
        transactionQueries.earliestCreatedAt()
            .asFlow()
            .mapToOne(Dispatchers.IO)
            .map { it.MIN }
}
