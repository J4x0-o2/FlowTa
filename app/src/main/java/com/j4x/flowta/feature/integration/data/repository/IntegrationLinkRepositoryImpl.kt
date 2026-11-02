package com.j4x.flowta.feature.integration.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.j4x.flowta.database.IntegrationLinkQueries
import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import com.j4x.flowta.feature.integration.domain.repository.IntegrationLinkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.j4x.flowta.database.Integration_link as IntegrationLinkRow

class IntegrationLinkRepositoryImpl @Inject constructor(
    private val queries: IntegrationLinkQueries
) : IntegrationLinkRepository {

    override suspend fun get(sourceApp: String): IntegrationLink? =
        withContext(Dispatchers.IO) {
            queries.selectBySource(sourceApp).executeAsOneOrNull()?.toDomain()
        }

    override fun observe(sourceApp: String): Flow<IntegrationLink?> =
        queries.selectBySource(sourceApp)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toDomain() }

    override suspend fun save(link: IntegrationLink) = withContext(Dispatchers.IO) {
        queries.upsert(link.sourceApp, link.incomeTypeId, link.cashAccountId, link.transferAccountId, link.createdAt)
    }

    override suspend fun delete(sourceApp: String) = withContext(Dispatchers.IO) {
        queries.deleteBySource(sourceApp)
    }

    private fun IntegrationLinkRow.toDomain() = IntegrationLink(
        sourceApp         = source_app,
        incomeTypeId      = income_type_id,
        cashAccountId     = cash_account_id,
        transferAccountId = transfer_account_id,
        createdAt         = created_at
    )
}
