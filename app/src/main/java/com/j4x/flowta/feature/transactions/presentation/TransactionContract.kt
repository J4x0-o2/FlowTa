package com.j4x.flowta.feature.transactions.presentation

import com.j4x.flowta.feature.transactions.domain.model.Transaction
import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer

sealed interface FeedItem {
    val id: String
    val createdAt: Long

    data class Tx(val transaction: Transaction) : FeedItem {
        override val id        = transaction.id
        override val createdAt = transaction.createdAt
    }

    data class Xfer(val transfer: InternalTransfer) : FeedItem {
        override val id        = "xfer_${transfer.id}"
        override val createdAt = transfer.createdAt
    }
}

data class TransactionState(
    val isLoading: Boolean = false,
    val feed: List<FeedItem> = emptyList(),
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
