package com.j4x.flowta.feature.accounts.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.accounts.domain.repository.AccountRepository
import com.j4x.flowta.feature.accounts.domain.usecase.CountAccountMovementsUseCase
import com.j4x.flowta.feature.accounts.domain.usecase.ResetAccountUseCase
import com.j4x.flowta.feature.transactions.domain.usecase.GetAccountTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val getAccountTransactions: GetAccountTransactionsUseCase,
    private val countAccountMovements: CountAccountMovementsUseCase,
    private val resetAccount: ResetAccountUseCase
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"])

    private val _state = MutableStateFlow(AccountDetailState())
    val state = _state.asStateFlow()

    private val _effects = Channel<AccountDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        accountRepository.getById(accountId)
            .filterNotNull()
            .onEach { account -> _state.update { it.copy(account = account) } }
            .launchIn(viewModelScope)

        getAccountTransactions(accountId)
            .onEach { list -> _state.update { it.copy(transactions = list) } }
            .launchIn(viewModelScope)
    }

    fun selectTransaction(id: String) = _state.update { it.copy(selectedTransactionId = id) }
    fun dismissDetail() = _state.update { it.copy(selectedTransactionId = null) }

    fun requestReset() {
        viewModelScope.launch {
            countAccountMovements(accountId)
                .onSuccess { n -> _state.update { it.copy(resetDialog = ResetAccountDialog(n)) } }
                .onFailure { _effects.send(AccountDetailEffect.ShowMessage(it.message ?: "Error")) }
        }
    }

    fun dismissReset() = _state.update { it.copy(resetDialog = null) }

    fun confirmReset(startingBalance: Double) {
        _state.update { it.copy(resetDialog = null) }
        viewModelScope.launch {
            resetAccount(accountId, startingBalance)
                .onSuccess { _effects.send(AccountDetailEffect.ShowMessage("Cuenta reiniciada")) }
                .onFailure { _effects.send(AccountDetailEffect.ShowMessage(it.message ?: "Error")) }
        }
    }
}
