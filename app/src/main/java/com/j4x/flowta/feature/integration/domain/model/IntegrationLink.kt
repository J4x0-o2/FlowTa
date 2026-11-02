package com.j4x.flowta.feature.integration.domain.model

/** Maps a source app's payment methods to FlowTa accounts and the income type its trips use. */
data class IntegrationLink(
    val sourceApp: String,
    val incomeTypeId: String,
    val cashAccountId: String,
    val transferAccountId: String,
    val createdAt: Long
)
