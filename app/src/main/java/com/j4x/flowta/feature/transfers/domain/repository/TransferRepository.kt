package com.j4x.flowta.feature.transfers.domain.repository

import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer
import com.j4x.flowta.feature.transfers.domain.model.NewInternalTransfer
import kotlinx.coroutines.flow.Flow

interface TransferRepository {
    fun getAll(): Flow<List<InternalTransfer>>
    suspend fun execute(transfer: NewInternalTransfer): Result<Unit>
}
