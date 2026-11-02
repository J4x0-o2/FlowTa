package com.j4x.flowta.feature.integration.data.repository

import com.j4x.flowta.database.AccountQueries
import com.j4x.flowta.database.TransactionQueries
import com.j4x.flowta.database.TransactionTypeQueries
import com.j4x.flowta.feature.integration.domain.model.ExternalIncome
import com.j4x.flowta.feature.integration.domain.model.ExternalPaymentMethod
import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import com.j4x.flowta.feature.integration.domain.model.RecordExternalIncomeResult
import com.j4x.flowta.feature.integration.domain.repository.ExternalIncomeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class ExternalIncomeRepositoryImpl @Inject constructor(
    private val transactionQueries: TransactionQueries,
    private val accountQueries: AccountQueries,
    private val typeQueries: TransactionTypeQueries
) : ExternalIncomeRepository {

    override suspend fun record(income: ExternalIncome, link: IntegrationLink): RecordExternalIncomeResult =
        withContext(Dispatchers.IO) {
            try {
                insertInTransaction(income, link)
            } catch (e: Exception) {
                // A concurrent call may have committed the same (source_app, external_id) between our
                // pre-check and the insert; the unique index then rejects ours. That is a Duplicate.
                if (isRecorded(income)) RecordExternalIncomeResult.Duplicate else throw e
            }
        }

    private fun isRecorded(income: ExternalIncome): Boolean =
        transactionQueries.countBySourceExternal(income.sourceApp, income.externalId).executeAsOne() > 0

    private fun insertInTransaction(income: ExternalIncome, link: IntegrationLink): RecordExternalIncomeResult =
        transactionQueries.transactionWithResult {
            if (isRecorded(income)) return@transactionWithResult RecordExternalIncomeResult.Duplicate

            val accountId = when (income.paymentMethod) {
                ExternalPaymentMethod.CASH     -> link.cashAccountId
                ExternalPaymentMethod.TRANSFER -> link.transferAccountId
            }
            val account = accountQueries.selectById(accountId).executeAsOneOrNull()
            val type = typeQueries.selectById(link.incomeTypeId).executeAsOneOrNull()
            if (account == null || type == null || type.category != "INCOME") {
                return@transactionWithResult RecordExternalIncomeResult.NotLinked
            }

            val amount = income.amountMinor / 100.0
            transactionQueries.insertExternal(
                id                  = UUID.randomUUID().toString(),
                amount              = amount,
                transaction_type_id = link.incomeTypeId,
                account_id          = accountId,
                note                = income.note,
                created_at          = income.occurredAt,
                source_app          = income.sourceApp,
                external_id         = income.externalId
            )
            accountQueries.updateBalance(account.balance + amount, accountId)
            RecordExternalIncomeResult.Recorded
        }
}
