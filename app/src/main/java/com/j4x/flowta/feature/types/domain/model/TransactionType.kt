package com.j4x.flowta.feature.types.domain.model

data class TransactionType(
    val id: String,
    val name: String,
    val category: TypeCategory,
    val createdAt: Long
)

enum class TypeCategory { INCOME, EXPENSE, RECURRING_EXPENSE, TRANSFER, INCOME_SOURCE }
