package com.j4x.flowta.feature.integration.presentation

import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.types.domain.model.TransactionType

const val DEFAULT_NEW_TYPE_NAME = "Carreras"

data class ConnectSourceState(
    val sourceApp: String? = null,
    val loaded: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val incomeTypes: List<TransactionType> = emptyList(),
    val selectedTypeId: String? = null,
    val creatingNewType: Boolean = false,
    val newTypeName: String = DEFAULT_NEW_TYPE_NAME,
    val cashAccountId: String? = null,
    val transferAccountId: String? = null,
    val isSaving: Boolean = false
) {
    val noAccounts: Boolean get() = loaded && accounts.isEmpty()

    private val typeChosen: Boolean
        get() = if (creatingNewType) newTypeName.isNotBlank() else selectedTypeId != null

    val canConnect: Boolean
        get() = loaded && !isSaving && sourceApp != null && typeChosen &&
            cashAccountId != null && transferAccountId != null
}

sealed interface ConnectSourceIntent {
    data class Initialize(val sourceApp: String) : ConnectSourceIntent
    data class SelectType(val id: String) : ConnectSourceIntent
    data object SelectNewType : ConnectSourceIntent
    data class SetNewTypeName(val text: String) : ConnectSourceIntent
    data class SelectCashAccount(val id: String) : ConnectSourceIntent
    data class SelectTransferAccount(val id: String) : ConnectSourceIntent
    data object Connect : ConnectSourceIntent
}

sealed interface ConnectSourceEffect {
    data object Connected : ConnectSourceEffect
    data class ShowError(val message: String) : ConnectSourceEffect
}
