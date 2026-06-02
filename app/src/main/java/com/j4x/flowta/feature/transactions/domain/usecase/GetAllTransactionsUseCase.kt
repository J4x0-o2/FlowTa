package com.j4x.flowta.feature.transactions.domain.usecase

import com.j4x.flowta.feature.transactions.domain.model.Transaction
import com.j4x.flowta.feature.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(): Flow<List<Transaction>> = repository.getAll()
}
