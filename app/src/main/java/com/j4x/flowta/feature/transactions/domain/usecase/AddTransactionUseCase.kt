package com.j4x.flowta.feature.transactions.domain.usecase

import com.j4x.flowta.feature.transactions.domain.model.NewTransaction
import com.j4x.flowta.feature.transactions.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    suspend operator fun invoke(transaction: NewTransaction): Result<Unit> =
        repository.add(transaction)
}
