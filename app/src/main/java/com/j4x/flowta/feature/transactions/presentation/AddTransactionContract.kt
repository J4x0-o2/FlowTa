package com.j4x.flowta.feature.transactions.presentation

import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.types.domain.model.TransactionType

enum class TransactionTab { INCOME, EXPENSE, TRANSFER }

data class AddTransactionState(
    val activeTab: TransactionTab = TransactionTab.EXPENSE,
    val amountText: String = "",
    val selectedTypeId: String? = null,
    val selectedAccountId: String? = null,
    val selectedFromAccountId: String? = null,
    val selectedToAccountId: String? = null,
    val selectedSourceTypeId: String? = null,
    val note: String = "",
    val showNoteField: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val incomeTypes: List<TransactionType> = emptyList(),
    val expenseTypes: List<TransactionType> = emptyList(),
    val transferTypes: List<TransactionType> = emptyList(),
    val incomeSourceTypes: List<TransactionType> = emptyList(),
    val isSubmitting: Boolean = false
)

sealed interface AddTransactionIntent {
    data class SelectTab(val tab: TransactionTab) : AddTransactionIntent
    data class SetAmount(val text: String) : AddTransactionIntent
    data class SelectType(val id: String) : AddTransactionIntent
    data class SelectAccount(val id: String) : AddTransactionIntent
    data class SelectFromAccount(val id: String) : AddTransactionIntent
    data class SelectToAccount(val id: String) : AddTransactionIntent
    data class SelectSourceType(val id: String) : AddTransactionIntent
    data class SetNote(val text: String) : AddTransactionIntent
    data object ToggleNoteField : AddTransactionIntent
    data object Submit : AddTransactionIntent
    data object Reset : AddTransactionIntent
}

sealed interface AddTransactionEffect {
    data object Dismiss : AddTransactionEffect
    data class ShowError(val message: String) : AddTransactionEffect
}
