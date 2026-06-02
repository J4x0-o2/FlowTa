package com.j4x.flowta.feature.types.presentation

import com.j4x.flowta.feature.types.domain.model.TransactionType

data class TypesState(
    val isLoading: Boolean = false,
    val incomeTypes: List<TransactionType> = emptyList(),
    val expenseTypes: List<TransactionType> = emptyList(),
    val transferTypes: List<TransactionType> = emptyList(),
    val error: String? = null
)

sealed interface TypesIntent {
    data class AddType(val name: String, val category: String) : TypesIntent
    data class DeleteType(val id: String) : TypesIntent
}

sealed interface TypesEffect {
    data class ShowError(val message: String) : TypesEffect
}
