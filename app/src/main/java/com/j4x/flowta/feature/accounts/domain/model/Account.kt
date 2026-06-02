package com.j4x.flowta.feature.accounts.domain.model

data class Account(
    val id: String,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val metaAmount: Double?,
    val metaPeriodicity: String?,
    val createdAt: Long
)

enum class AccountType { CASH, BANK, DIGITAL_WALLET, SAVINGS, OTHER }
