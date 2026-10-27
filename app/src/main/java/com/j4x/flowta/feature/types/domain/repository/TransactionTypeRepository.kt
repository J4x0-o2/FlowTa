package com.j4x.flowta.feature.types.domain.repository

import com.j4x.flowta.feature.types.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface TransactionTypeRepository {
    fun getAll(): Flow<List<TransactionType>>
    fun getByCategory(category: String): Flow<List<TransactionType>>
    suspend fun add(name: String, category: String): Result<Unit>
    /**
     * Inserts every (name, category) pair only if no type exists, checking and inserting inside ONE
     * transaction: all or nothing, and overlapping callers cannot both seed.
     */
    suspend fun seedIfEmpty(types: List<Pair<String, String>>): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
