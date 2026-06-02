package com.j4x.flowta.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {

    private val _state   = MutableStateFlow(ProfileState())
    val state = _state.asStateFlow()

    private val _effects = Channel<ProfileEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        combine(
            repository.getAccounts(),
            repository.getTotalCapital(),
            repository.getTransfers()
        ) { accounts, capital, transfers ->
            _state.update { it.copy(accounts = accounts, totalCapital = capital, transfers = transfers) }
        }.launchIn(viewModelScope)
    }

    fun onIntent(intent: ProfileIntent) {
        when (intent) {
            is ProfileIntent.NavigateToAccount  -> _effects.trySend(ProfileEffect.NavigateToAccount(intent.accountId))
            is ProfileIntent.NavigateToSettings -> _effects.trySend(ProfileEffect.NavigateToSettings)
            is ProfileIntent.NavigateToTypes    -> _effects.trySend(ProfileEffect.NavigateToTypes)
        }
    }
}
