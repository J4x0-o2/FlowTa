package com.j4x.flowta.feature.transfers.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.transfers.domain.model.NewInternalTransfer
import com.j4x.flowta.feature.transfers.domain.usecase.ExecuteInternalTransferUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val executeTransfer: ExecuteInternalTransferUseCase
) : ViewModel() {

    private val _state   = MutableStateFlow(TransferState())
    val state = _state.asStateFlow()

    private val _effects = Channel<TransferEffect>()
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: TransferIntent) {
        when (intent) {
            is TransferIntent.Execute -> viewModelScope.launch {
                executeTransfer(
                    NewInternalTransfer(
                        fromAccountId  = intent.fromAccountId,
                        toAccountId    = intent.toAccountId,
                        transferTypeId = null,
                        amount         = intent.amount,
                        note           = intent.note
                    )
                ).fold(
                    onSuccess = { _effects.send(TransferEffect.TransferSuccess) },
                    onFailure = { _effects.send(TransferEffect.ShowError(it.message ?: "Error")) }
                )
            }
        }
    }
}
