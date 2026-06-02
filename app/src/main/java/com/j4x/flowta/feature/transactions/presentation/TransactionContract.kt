package com.j4x.flowta.feature.transactions.presentation

import com.j4x.flowta.feature.transactions.domain.model.Transaction

data class TransactionState(
    val isLoading: Boolean = false,
    val transactions: List<Transaction> = emptyList(),
    val selectedTransactionId: String? = null,
    val balance: Double = 0.0,
    val error: String? = null
)

sealed interface TransactionIntent {
    data object LoadTransactions : TransactionIntent
    data class SelectTransaction(val id: String) : TransactionIntent
    data object DismissDetail : TransactionIntent
    data object OpenAddForm : TransactionIntent
}

sealed interface TransactionEffect {
    data object NavigateToAddForm : TransactionEffect
    data class ShowError(val message: String) : TransactionEffect
}
