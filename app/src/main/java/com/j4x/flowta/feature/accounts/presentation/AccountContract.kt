package com.j4x.flowta.feature.accounts.presentation

import com.j4x.flowta.feature.accounts.domain.model.Account

data class AccountState(
    val isLoading: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val totalCapital: Double = 0.0,
    val error: String? = null
)

sealed interface AccountIntent {
    data object LoadAccounts : AccountIntent
    data class AddAccount(
        val name: String,
        val type: String,
        val initialBalance: Double,
        val metaAmount: Double?,
        val metaPeriodicity: String?
    ) : AccountIntent
    data class DeleteAccount(val id: String) : AccountIntent
    data class NavigateToDetail(val accountId: String) : AccountIntent
}

sealed interface AccountEffect {
    data class NavigateToDetail(val accountId: String) : AccountEffect
    data class ShowError(val message: String) : AccountEffect
}
