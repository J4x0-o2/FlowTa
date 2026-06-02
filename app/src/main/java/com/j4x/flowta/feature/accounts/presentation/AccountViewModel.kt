package com.j4x.flowta.feature.accounts.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.accounts.domain.usecase.AddAccountUseCase
import com.j4x.flowta.feature.accounts.domain.usecase.DeleteAccountUseCase
import com.j4x.flowta.feature.accounts.domain.usecase.GetAccountsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val getAccounts: GetAccountsUseCase,
    private val addAccount: AddAccountUseCase,
    private val deleteAccount: DeleteAccountUseCase
) : ViewModel() {

    private val _state   = MutableStateFlow(AccountState())
    val state = _state.asStateFlow()

    private val _effects = Channel<AccountEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        getAccounts()
            .onEach { list ->
                _state.update {
                    it.copy(
                        isLoading    = false,
                        accounts     = list,
                        totalCapital = list.sumOf { acc -> acc.balance }
                    )
                }
            }
            .catch { e -> _state.update { it.copy(error = e.message) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: AccountIntent) {
        when (intent) {
            is AccountIntent.AddAccount -> viewModelScope.launch {
                addAccount(intent.name, intent.type, intent.initialBalance, intent.metaAmount, intent.metaPeriodicity)
                    .onFailure { _effects.send(AccountEffect.ShowError(it.message ?: "Error")) }
            }
            is AccountIntent.DeleteAccount -> viewModelScope.launch {
                deleteAccount(intent.id)
                    .onFailure { _effects.send(AccountEffect.ShowError(it.message ?: "Error")) }
            }
            is AccountIntent.NavigateToDetail ->
                _effects.trySend(AccountEffect.NavigateToDetail(intent.accountId))
            else -> Unit
        }
    }
}
