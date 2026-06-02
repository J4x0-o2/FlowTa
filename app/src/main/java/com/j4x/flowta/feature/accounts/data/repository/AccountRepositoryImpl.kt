package com.j4x.flowta.feature.accounts.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.feature.accounts.data.mapper.toDomain
import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.accounts.domain.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val queries: AccountQueries
) : AccountRepository {

    override fun getAll(): Flow<List<Account>> =
        queries.selectAll().asFlow().mapToList(Dispatchers.IO).map { it.map { row -> row.toDomain() } }

    override fun getById(id: String): Flow<Account?> =
        queries.selectById(id).asFlow().mapToOneOrNull(Dispatchers.IO).map { it?.toDomain() }

    override fun getTotalBalance(): Flow<Double> =
        queries.totalBalance().asFlow().mapToOneOrNull(Dispatchers.IO).map { it ?: 0.0 }

    override suspend fun add(
        name: String,
        type: String,
        initialBalance: Double,
        metaAmount: Double?,
        metaPeriodicity: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            queries.insert(
                id               = UUID.randomUUID().toString(),
                name             = name,
                type             = type,
                balance          = initialBalance,
                meta_amount      = metaAmount,
                meta_periodicity = metaPeriodicity,
                created_at       = System.currentTimeMillis()
            )
        }
    }

    override suspend fun delete(id: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { queries.delete(id) } }
}
