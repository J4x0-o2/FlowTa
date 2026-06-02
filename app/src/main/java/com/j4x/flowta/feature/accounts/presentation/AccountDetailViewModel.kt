package com.j4x.flowta.feature.accounts.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.accounts.domain.repository.AccountRepository
import com.j4x.flowta.feature.transactions.domain.model.Transaction
import com.j4x.flowta.feature.transactions.domain.usecase.GetAccountTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AccountDetailState(
    val account: Account? = null,
    val transactions: List<Transaction> = emptyList(),
    val selectedTransactionId: String? = null
)

@HiltViewModel
class AccountDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountRepository: AccountRepository,
    private val getAccountTransactions: GetAccountTransactionsUseCase
) : ViewModel() {

    private val accountId: String = checkNotNull(savedStateHandle["accountId"])

    private val _state = MutableStateFlow(AccountDetailState())
    val state = _state.asStateFlow()

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
}
