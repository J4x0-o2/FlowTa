package com.j4x.flowta.feature.transactions.domain.model

data class Transaction(
    val id: String,
    val amount: Double,
    val flowType: String,
    val note: String?,
    val createdAt: Long,
    val accountId: String,
    val accountName: String,
    val transactionTypeId: String,
    val typeName: String,
    val typeCategory: String,
    val sourceTypeId: String?,
    val sourceTypeName: String?
)

data class NewTransaction(
    val amount: Double,
    val flowType: String,
    val transactionTypeId: String,
    val sourceTypeId: String?,
    val accountId: String,
    val note: String?
)
