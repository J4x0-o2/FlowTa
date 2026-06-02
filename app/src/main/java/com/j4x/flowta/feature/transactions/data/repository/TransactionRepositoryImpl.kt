package com.j4x.flowta.feature.transactions.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.database.TransactionQueries
import com.j4x.flowta.feature.transactions.data.mapper.toDomain
import com.j4x.flowta.feature.transactions.domain.model.NewTransaction
import com.j4x.flowta.feature.transactions.domain.model.Transaction
import com.j4x.flowta.feature.transactions.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionQueries: TransactionQueries,
    private val accountQueries: AccountQueries
) : TransactionRepository {

    override fun getAll(): Flow<List<Transaction>> =
        transactionQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override fun getByAccount(accountId: String): Flow<List<Transaction>> =
        transactionQueries.selectByAccount(accountId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list -> list.map { it.toDomain() } }

    override suspend fun add(transaction: NewTransaction): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                transactionQueries.transaction {
                    transactionQueries.insert(
                        id                  = UUID.randomUUID().toString(),
                        amount              = transaction.amount,
                        flow_type           = transaction.flowType,
                        transaction_type_id = transaction.transactionTypeId,
                        source_type_id      = transaction.sourceTypeId,
                        account_id          = transaction.accountId,
                        note                = transaction.note,
                        created_at          = System.currentTimeMillis()
                    )
                    val account = accountQueries.selectById(transaction.accountId).executeAsOne()
                    val delta = if (transaction.flowType == "INCOME") transaction.amount else -transaction.amount
                    accountQueries.updateBalance(account.balance + delta, transaction.accountId)
                }
            }
        }

    override suspend fun delete(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching { transactionQueries.delete(id) }
        }
}
