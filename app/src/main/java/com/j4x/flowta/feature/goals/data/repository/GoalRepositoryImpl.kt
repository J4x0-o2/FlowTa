package com.j4x.flowta.feature.goals.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.database.GoalContributionQueries
import com.j4x.flowta.database.GoalQueries
import com.j4x.flowta.feature.goals.domain.model.Goal
import com.j4x.flowta.feature.goals.domain.model.GoalContribution
import com.j4x.flowta.feature.goals.domain.model.GoalMode
import com.j4x.flowta.feature.goals.domain.model.GoalPeriodicity
import com.j4x.flowta.feature.goals.domain.repository.GoalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class GoalRepositoryImpl @Inject constructor(
    private val goalQueries: GoalQueries,
    private val contributionQueries: GoalContributionQueries,
    private val accountQueries: AccountQueries
) : GoalRepository {

    override fun getAll(): Flow<List<Goal>> =
        goalQueries.selectAll().asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { row ->
                Goal(row.id, row.name, row.target_amount, row.current_amount,
                    GoalMode.valueOf(row.mode), GoalPeriodicity.valueOf(row.periodicity),
                    row.deadline, row.created_at)
            }
        }

    override fun getById(id: String): Flow<Goal?> =
        goalQueries.selectById(id).asFlow().mapToOneOrNull(Dispatchers.IO).map { row ->
            row?.let {
                Goal(it.id, it.name, it.target_amount, it.current_amount,
                    GoalMode.valueOf(it.mode), GoalPeriodicity.valueOf(it.periodicity),
                    it.deadline, it.created_at)
            }
        }

    override fun getContributions(goalId: String): Flow<List<GoalContribution>> =
        contributionQueries.selectByGoal(goalId).asFlow().mapToList(Dispatchers.IO).map { list ->
            list.map { row -> GoalContribution(row.id, row.amount, row.note, row.created_at, row.from_account_name) }
        }

    override suspend fun add(name: String, targetAmount: Double, mode: String, periodicity: String, deadline: Long?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                goalQueries.insert(UUID.randomUUID().toString(), name, targetAmount, 0.0, mode, periodicity, deadline, System.currentTimeMillis())
            }
        }

    override suspend fun addContribution(goalId: String, fromAccountId: String, amount: Double, note: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val account = accountQueries.selectById(fromAccountId).executeAsOne()
                if (account.balance < amount) error("Saldo insuficiente")
                goalQueries.transaction {
                    accountQueries.updateBalance(account.balance - amount, fromAccountId)
                    goalQueries.updateCurrentAmount(amount, goalId)
                    contributionQueries.insert(UUID.randomUUID().toString(), goalId, fromAccountId, amount, note, System.currentTimeMillis())
                }
            }
        }

    override suspend fun delete(id: String): Result<Unit> =
        withContext(Dispatchers.IO) { runCatching { goalQueries.delete(id) } }
}
