package com.j4x.flowta.feature.transactions.domain.repository

import com.j4x.flowta.feature.transactions.domain.model.NewTransaction
import com.j4x.flowta.feature.transactions.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAll(): Flow<List<Transaction>>
    fun getByAccount(accountId: String): Flow<List<Transaction>>
    suspend fun add(transaction: NewTransaction): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
