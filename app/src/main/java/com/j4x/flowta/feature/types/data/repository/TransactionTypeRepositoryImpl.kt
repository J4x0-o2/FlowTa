package com.j4x.flowta.feature.types.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.j4x.flowta.database.TransactionTypeQueries
import com.j4x.flowta.feature.types.data.mapper.toDomain
import com.j4x.flowta.feature.types.domain.model.TransactionType
import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class TransactionTypeRepositoryImpl @Inject constructor(
    private val queries: TransactionTypeQueries
) : TransactionTypeRepository {

    override fun getAll(): Flow<List<TransactionType>> =
        queries.selectAll().asFlow().mapToList(Dispatchers.IO).map { it.map { row -> row.toDomain() } }

    override fun getByCategory(category: String): Flow<List<TransactionType>> =
        queries.selectByCategory(category).asFlow().mapToList(Dispatchers.IO).map { it.map { row -> row.toDomain() } }

    override suspend fun add(name: String, category: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                queries.insert(UUID.randomUUID().toString(), name, category, System.currentTimeMillis())
            }
        }

    override suspend fun delete(id: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { queries.delete(id) } }
}
