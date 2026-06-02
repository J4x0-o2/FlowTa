package com.j4x.flowta.feature.transfers.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.database.InternalTransferQueries
import com.j4x.flowta.feature.transfers.data.mapper.toDomain
import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer
import com.j4x.flowta.feature.transfers.domain.model.NewInternalTransfer
import com.j4x.flowta.feature.transfers.domain.repository.TransferRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class TransferRepositoryImpl @Inject constructor(
    private val transferQueries: InternalTransferQueries,
    private val accountQueries: AccountQueries
) : TransferRepository {

    override fun getAll(): Flow<List<InternalTransfer>> =
        transferQueries.selectAll().asFlow().mapToList(Dispatchers.IO).map { it.map { row -> row.toDomain() } }

    override suspend fun execute(transfer: NewInternalTransfer): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val fromAccount = accountQueries.selectById(transfer.fromAccountId).executeAsOne()
                if (fromAccount.balance < transfer.amount) {
                    error("Saldo insuficiente en la cuenta origen")
                }
                transferQueries.transaction {
                    accountQueries.updateBalance(fromAccount.balance - transfer.amount, transfer.fromAccountId)
                    val toAccount = accountQueries.selectById(transfer.toAccountId).executeAsOne()
                    accountQueries.updateBalance(toAccount.balance + transfer.amount, transfer.toAccountId)
                    transferQueries.insert(
                        id               = UUID.randomUUID().toString(),
                        from_account_id  = transfer.fromAccountId,
                        to_account_id    = transfer.toAccountId,
                        transfer_type_id = transfer.transferTypeId,
                        amount           = transfer.amount,
                        note             = transfer.note,
                        created_at       = System.currentTimeMillis()
                    )
                }
            }
        }
}
