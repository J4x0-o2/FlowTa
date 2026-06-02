package com.j4x.flowta.feature.profile.presentation

import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer

data class ProfileState(
    val isLoading: Boolean = false,
    val totalCapital: Double = 0.0,
    val accounts: List<Account> = emptyList(),
    val transfers: List<InternalTransfer> = emptyList()
)

sealed interface ProfileIntent {
    data class NavigateToAccount(val accountId: String) : ProfileIntent
    data object NavigateToSettings : ProfileIntent
    data object NavigateToTypes : ProfileIntent
}

sealed interface ProfileEffect {
    data class NavigateToAccount(val accountId: String) : ProfileEffect
    data object NavigateToSettings : ProfileEffect
    data object NavigateToTypes : ProfileEffect
}
