package com.j4x.flowta.feature.integration.domain.repository

import com.j4x.flowta.feature.integration.domain.model.ExternalIncome
import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import com.j4x.flowta.feature.integration.domain.model.RecordExternalIncomeResult

interface ExternalIncomeRepository {
    /** Dedupes, resolves the mapped account and writes the income plus balance update atomically. */
    suspend fun record(income: ExternalIncome, link: IntegrationLink): RecordExternalIncomeResult
}
