package com.j4x.flowta.feature.goals.domain.repository

import com.j4x.flowta.feature.goals.domain.model.Goal
import com.j4x.flowta.feature.goals.domain.model.GoalContribution
import kotlinx.coroutines.flow.Flow

interface GoalRepository {
    fun getAll(): Flow<List<Goal>>
    fun getById(id: String): Flow<Goal?>
    fun getContributions(goalId: String): Flow<List<GoalContribution>>
    suspend fun add(name: String, targetAmount: Double, mode: String, periodicity: String, deadline: Long?): Result<Unit>
    suspend fun addContribution(goalId: String, fromAccountId: String, amount: Double, note: String?): Result<Unit>
    suspend fun delete(id: String): Result<Unit>
}
