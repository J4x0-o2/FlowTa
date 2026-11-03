package com.j4x.flowta.feature.integration.domain.usecase

import com.j4x.flowta.feature.integration.domain.model.ExternalIncome
import com.j4x.flowta.feature.integration.domain.model.RecordExternalIncomeResult
import com.j4x.flowta.feature.integration.domain.repository.ExternalIncomeRepository
import com.j4x.flowta.feature.integration.domain.repository.IntegrationLinkRepository
import javax.inject.Inject

class RecordExternalIncomeUseCase @Inject constructor(
    private val links: IntegrationLinkRepository,
    private val externalIncomes: ExternalIncomeRepository
) {
    suspend operator fun invoke(income: ExternalIncome): RecordExternalIncomeResult {
        // The unique index only dedupes when both source columns are non-null, so reject blanks here.
        if (income.sourceApp.isBlank()) return RecordExternalIncomeResult.Invalid("source_app is blank")
        if (income.externalId.isBlank()) return RecordExternalIncomeResult.Invalid("external_id is blank")
        if (income.amountMinor <= 0) return RecordExternalIncomeResult.Invalid("amount_minor must be positive")
        if (income.occurredAt <= 0) return RecordExternalIncomeResult.Invalid("occurred_at must be positive")

        val link = links.get(income.sourceApp) ?: return RecordExternalIncomeResult.NotLinked
        return externalIncomes.record(income, link)
    }
}
