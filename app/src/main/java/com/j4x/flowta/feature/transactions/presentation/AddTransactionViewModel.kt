package com.j4x.flowta.feature.transactions.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.accounts.domain.usecase.GetAccountsUseCase
import com.j4x.flowta.feature.transactions.domain.model.NewTransaction
import com.j4x.flowta.feature.transactions.domain.usecase.AddTransactionUseCase
import com.j4x.flowta.feature.transfers.domain.model.NewInternalTransfer
import com.j4x.flowta.feature.transfers.domain.usecase.ExecuteInternalTransferUseCase
import com.j4x.flowta.feature.types.domain.usecase.GetTransactionTypesByCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val getAccounts: GetAccountsUseCase,
    private val getTypesByCategory: GetTransactionTypesByCategoryUseCase,
    private val addTransaction: AddTransactionUseCase,
    private val executeTransfer: ExecuteInternalTransferUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AddTransactionState())
    val state = _state.asStateFlow()

    private val _effects = Channel<AddTransactionEffect>()
    val effects = _effects.receiveAsFlow()

    init {
        getAccounts()
            .onEach { list -> _state.update { it.copy(accounts = list) } }
            .launchIn(viewModelScope)

        combine(
            getTypesByCategory("INCOME"),
            getTypesByCategory("EXPENSE"),
            getTypesByCategory("TRANSFER")
        ) { income, expense, transfer ->
            _state.update { it.copy(incomeTypes = income, expenseTypes = expense, transferTypes = transfer) }
        }.launchIn(viewModelScope)

        getTypesByCategory("INCOME_SOURCE")
            .onEach { list -> _state.update { it.copy(incomeSourceTypes = list) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: AddTransactionIntent) {
        when (intent) {
            is AddTransactionIntent.SelectTab -> _state.update { s ->
                AddTransactionState(
                    activeTab = intent.tab,
                    accounts = s.accounts,
                    incomeTypes = s.incomeTypes,
                    expenseTypes = s.expenseTypes,
                    transferTypes = s.transferTypes,
                    incomeSourceTypes = s.incomeSourceTypes
                )
            }
            is AddTransactionIntent.SetAmount ->
                _state.update { it.copy(amountText = intent.text) }
            is AddTransactionIntent.SelectType ->
                _state.update { it.copy(selectedTypeId = intent.id) }
            is AddTransactionIntent.SelectAccount ->
                _state.update { it.copy(selectedAccountId = intent.id) }
            is AddTransactionIntent.SelectFromAccount ->
                _state.update { it.copy(selectedFromAccountId = intent.id, selectedToAccountId = null) }
            is AddTransactionIntent.SelectToAccount ->
                _state.update { it.copy(selectedToAccountId = intent.id) }
            is AddTransactionIntent.SelectSourceType ->
                _state.update { it.copy(selectedSourceTypeId = intent.id) }
            is AddTransactionIntent.SetNote ->
                _state.update { it.copy(note = intent.text) }
            is AddTransactionIntent.ToggleNoteField ->
                _state.update { it.copy(showNoteField = !it.showNoteField, note = "") }
            is AddTransactionIntent.Submit -> handleSubmit()
            is AddTransactionIntent.Reset -> _state.update { s ->
                AddTransactionState(
                    accounts = s.accounts,
                    incomeTypes = s.incomeTypes,
                    expenseTypes = s.expenseTypes,
                    transferTypes = s.transferTypes,
                    incomeSourceTypes = s.incomeSourceTypes
                )
            }
        }
    }

    private fun handleSubmit() {
        val s = _state.value
        val amount = s.amountText.toDoubleOrNull()?.takeIf { it > 0 } ?: return
        _state.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            val result = when (s.activeTab) {
                TransactionTab.INCOME -> {
                    val typeId = s.selectedTypeId ?: run { _state.update { it.copy(isSubmitting = false) }; return@launch }
                    val accountId = s.selectedAccountId ?: run { _state.update { it.copy(isSubmitting = false) }; return@launch }
                    addTransaction(
                        NewTransaction(
                            amount = amount,
                            flowType = "INCOME",
                            transactionTypeId = typeId,
                            sourceTypeId = s.selectedSourceTypeId,
                            accountId = accountId,
                            note = s.note.trim().ifEmpty { null }
                        )
                    )
                }
                TransactionTab.EXPENSE -> {
                    val typeId = s.selectedTypeId ?: run { _state.update { it.copy(isSubmitting = false) }; return@launch }
                    val accountId = s.selectedAccountId ?: run { _state.update { it.copy(isSubmitting = false) }; return@launch }
                    addTransaction(
                        NewTransaction(
                            amount = amount,
                            flowType = "EXPENSE",
                            transactionTypeId = typeId,
                            sourceTypeId = null,
                            accountId = accountId,
                            note = s.note.trim().ifEmpty { null }
                        )
                    )
                }
                TransactionTab.TRANSFER -> {
                    val fromId = s.selectedFromAccountId ?: run { _state.update { it.copy(isSubmitting = false) }; return@launch }
                    val toId = s.selectedToAccountId ?: run { _state.update { it.copy(isSubmitting = false) }; return@launch }
                    executeTransfer(
                        NewInternalTransfer(
                            fromAccountId = fromId,
                            toAccountId = toId,
                            transferTypeId = s.selectedTypeId,
                            amount = amount,
                            note = s.note.trim().ifEmpty { null }
                        )
                    )
                }
            }

            _state.update { it.copy(isSubmitting = false) }
            result.fold(
                onSuccess = { _effects.send(AddTransactionEffect.Dismiss) },
                onFailure = { e -> _effects.send(AddTransactionEffect.ShowError(e.message ?: "Error al registrar")) }
            )
        }
    }
}
