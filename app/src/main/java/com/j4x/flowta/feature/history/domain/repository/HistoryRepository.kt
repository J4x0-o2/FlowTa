package com.j4x.flowta.feature.history.domain.repository

import com.j4x.flowta.feature.history.domain.model.HistoryMovement
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    /** Income and expense movements with `fromMillis <= createdAt < toMillis`, oldest first. Transfers are not included. */
    fun observeMovements(fromMillis: Long, toMillis: Long): Flow<List<HistoryMovement>>

    /** Creation time of the oldest movement overall, null when there is none. */
    fun observeEarliest(): Flow<Long?>
}
