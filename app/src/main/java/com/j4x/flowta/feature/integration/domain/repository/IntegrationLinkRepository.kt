package com.j4x.flowta.feature.integration.domain.repository

import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import kotlinx.coroutines.flow.Flow

interface IntegrationLinkRepository {
    suspend fun get(sourceApp: String): IntegrationLink?
    fun observe(sourceApp: String): Flow<IntegrationLink?>
    suspend fun save(link: IntegrationLink)
    suspend fun delete(sourceApp: String)
}
