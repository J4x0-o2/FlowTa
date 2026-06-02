package com.j4x.flowta.feature.accounts.domain.repository

import com.j4x.flowta.feature.accounts.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAll(): Flow<List<Account>>
    fun getById(id: String): Flow<Account?>
    fun getTotalBalance(): Flow<Double>
    suspend fun add(
        name: String,
        type: String,
        initialBalance: Double,
        metaAmount: Double?,
        metaPeriodicity: String?
    ): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
