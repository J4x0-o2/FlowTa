package com.j4x.flowta.feature.transfers.domain.usecase

import com.j4x.flowta.feature.transfers.domain.model.NewInternalTransfer
import com.j4x.flowta.feature.transfers.domain.repository.TransferRepository
import javax.inject.Inject

class ExecuteInternalTransferUseCase @Inject constructor(
    private val repository: TransferRepository
) {
    suspend operator fun invoke(transfer: NewInternalTransfer): Result<Unit> =
        repository.execute(transfer)
}
