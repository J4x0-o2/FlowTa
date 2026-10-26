package com.j4x.flowta.feature.settings.data.repository

import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.database.GoalContributionQueries
import com.j4x.flowta.database.GoalQueries
import com.j4x.flowta.database.InternalTransferQueries
import com.j4x.flowta.database.TransactionQueries
import com.j4x.flowta.database.TransactionTypeQueries
import com.j4x.flowta.feature.settings.domain.repository.AppDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppDataRepositoryImpl @Inject constructor(
    private val accountQueries: AccountQueries,
    private val transactionQueries: TransactionQueries,
    private val transferQueries: InternalTransferQueries,
    private val typeQueries: TransactionTypeQueries,
    private val goalQueries: GoalQueries,
    private val contributionQueries: GoalContributionQueries
) : AppDataRepository {

    override suspend fun wipeAll(): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                accountQueries.transaction {
                    // Children first so foreign keys never dangle: rows referencing goal/account/type
                    // go before the rows they reference.
                    contributionQueries.deleteAll()
                    transactionQueries.deleteAll()
                    transferQueries.deleteAll()
                    goalQueries.deleteAll()
                    typeQueries.deleteAll()
                    accountQueries.deleteAll()
                }
            }
        }
}
