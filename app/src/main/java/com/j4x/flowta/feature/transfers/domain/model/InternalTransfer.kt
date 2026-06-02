package com.j4x.flowta.feature.transfers.domain.model

data class InternalTransfer(
    val id: String,
    val amount: Double,
    val note: String?,
    val createdAt: Long,
    val fromAccountId: String,
    val fromAccountName: String,
    val toAccountId: String,
    val toAccountName: String,
    val transferTypeId: String?
)

data class NewInternalTransfer(
    val fromAccountId: String,
    val toAccountId: String,
    val transferTypeId: String?,
    val amount: Double,
    val note: String?
)
