package com.j4x.flowta.feature.transfers.presentation

import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer

data class TransferState(
    val isLoading: Boolean = false,
    val transfers: List<InternalTransfer> = emptyList(),
    val error: String? = null
)

sealed interface TransferIntent {
    data class Execute(
        val fromAccountId: String,
        val toAccountId: String,
        val amount: Double,
        val note: String?
    ) : TransferIntent
}

sealed interface TransferEffect {
    data object TransferSuccess : TransferEffect
    data class ShowError(val message: String) : TransferEffect
}
