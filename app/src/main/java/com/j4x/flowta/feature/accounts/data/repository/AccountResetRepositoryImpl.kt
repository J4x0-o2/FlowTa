package com.j4x.flowta.feature.accounts.data.repository

import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.database.GoalContributionQueries
import com.j4x.flowta.database.GoalQueries
import com.j4x.flowta.database.InternalTransferQueries
import com.j4x.flowta.database.TransactionQueries
import com.j4x.flowta.feature.accounts.domain.repository.AccountResetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AccountResetRepositoryImpl @Inject constructor(
    private val accountQueries: AccountQueries,
    private val transactionQueries: TransactionQueries,
    private val transferQueries: InternalTransferQueries,
    private val goalQueries: GoalQueries,
    private val contributionQueries: GoalContributionQueries
) : AccountResetRepository {

    override suspend fun countMovements(accountId: String): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                transactionQueries.countByAccount(accountId).executeAsOne() +
                    transferQueries.countByAccount(accountId).executeAsOne()
            }
        }

    override suspend fun reset(accountId: String, startingBalance: Double): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                accountQueries.transaction {
                    // Goal contributions reference the account (from_account_id): take them back
                    // from the goals' progress, then delete them.
                    goalQueries.subtractAccountContributions(accountId)
                    contributionQueries.deleteByAccount(accountId)
                    transactionQueries.deleteByAccount(accountId)
                    transferQueries.deleteByAccount(accountId)
                    accountQueries.updateBalance(startingBalance, accountId)
                }
            }
        }
}
