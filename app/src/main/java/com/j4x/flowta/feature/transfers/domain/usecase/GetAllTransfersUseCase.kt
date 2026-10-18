package com.j4x.flowta.feature.transfers.domain.usecase

import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer
import com.j4x.flowta.feature.transfers.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTransfersUseCase @Inject constructor(
    private val repository: TransferRepository
) {
    operator fun invoke(): Flow<List<InternalTransfer>> = repository.getAll()
}
