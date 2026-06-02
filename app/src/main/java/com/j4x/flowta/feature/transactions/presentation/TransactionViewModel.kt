package com.j4x.flowta.feature.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.accounts.domain.usecase.GetTotalCapitalUseCase
import com.j4x.flowta.feature.transactions.domain.usecase.GetAllTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getAllTransactions: GetAllTransactionsUseCase,
    private val getTotalCapital: GetTotalCapitalUseCase
) : ViewModel() {

    private val _state  = MutableStateFlow(TransactionState())
    val state = _state.asStateFlow()

    private val _effects = Channel<TransactionEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        getAllTransactions()
            .onEach { list -> _state.update { it.copy(isLoading = false, transactions = list) } }
            .catch { e -> _state.update { it.copy(error = e.message) } }
            .launchIn(viewModelScope)

        getTotalCapital()
            .onEach { total -> _state.update { it.copy(balance = total) } }
            .catch { }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: TransactionIntent) {
        when (intent) {
            is TransactionIntent.SelectTransaction ->
                _state.update { it.copy(selectedTransactionId = intent.id) }
            is TransactionIntent.DismissDetail ->
                _state.update { it.copy(selectedTransactionId = null) }
            is TransactionIntent.OpenAddForm ->
                _effects.trySend(TransactionEffect.NavigateToAddForm)
            else -> Unit
        }
    }
}
