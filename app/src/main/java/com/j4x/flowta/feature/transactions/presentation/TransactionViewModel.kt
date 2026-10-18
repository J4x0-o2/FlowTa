package com.j4x.flowta.feature.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.accounts.domain.usecase.GetTotalCapitalUseCase
import com.j4x.flowta.feature.transactions.domain.usecase.GetAllTransactionsUseCase
import com.j4x.flowta.feature.transfers.domain.usecase.GetAllTransfersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val getAllTransactions: GetAllTransactionsUseCase,
    private val getAllTransfers: GetAllTransfersUseCase,
    private val getTotalCapital: GetTotalCapitalUseCase
) : ViewModel() {

    private val _state  = MutableStateFlow(TransactionState())
    val state = _state.asStateFlow()

    private val _effects = Channel<TransactionEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        combine(getAllTransactions(), getAllTransfers()) { transactions, transfers ->
            // Orden descendente (más reciente primero). Con reverseLayout = true en la lista,
            // la transacción más nueva queda anclada abajo y el historial cascadea hacia arriba,
            // como un chat.
            (transactions.map { FeedItem.Tx(it) } + transfers.map { FeedItem.Xfer(it) })
                .sortedByDescending { it.createdAt }
        }
            .onEach { feed -> _state.update { it.copy(isLoading = false, feed = feed) } }
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
