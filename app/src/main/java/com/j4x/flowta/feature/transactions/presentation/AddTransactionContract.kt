package com.j4x.flowta.feature.transactions.presentation

import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.format.parseAmount
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
    val isSubmitting: Boolean = false,
    val currencyCode: CurrencyCode = CurrencyCode.COP
) {
    /** Typed amount interpreted by [currencyCode]; null when invalid or ambiguous (blank parses to 0). */
    val parsedAmount: Double? get() = parseAmount(amountText, currencyCode)

    /** True only when text was typed and it cannot be interpreted (blank is not an error). */
    val amountInvalid: Boolean get() = amountText.isNotBlank() && parsedAmount == null

    /** Required fields for the active tab are filled and the amount is a valid positive value. */
    val canSubmit: Boolean
        get() {
            val amount = parsedAmount ?: return false
            if (amount <= 0) return false
            return when (activeTab) {
                TransactionTab.INCOME, TransactionTab.EXPENSE -> selectedTypeId != null && selectedAccountId != null
                TransactionTab.TRANSFER -> selectedFromAccountId != null && selectedToAccountId != null
            }
        }
}

sealed interface AddTransactionIntent {
    data class SelectTab(val tab: TransactionTab) : AddTransactionIntent
    data class SetCurrency(val code: CurrencyCode) : AddTransactionIntent
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
