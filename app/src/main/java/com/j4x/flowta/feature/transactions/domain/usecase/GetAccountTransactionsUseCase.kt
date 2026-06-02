package com.j4x.flowta.feature.transactions.domain.usecase

import com.j4x.flowta.feature.transactions.domain.model.Transaction
import com.j4x.flowta.feature.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAccountTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {
    operator fun invoke(accountId: String): Flow<List<Transaction>> =
        repository.getByAccount(accountId)
}
