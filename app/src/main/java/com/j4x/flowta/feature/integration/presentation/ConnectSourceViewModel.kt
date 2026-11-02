package com.j4x.flowta.feature.integration.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.accounts.domain.model.AccountType
import com.j4x.flowta.feature.accounts.domain.usecase.GetAccountsUseCase
import com.j4x.flowta.feature.integration.domain.model.IntegrationLink
import com.j4x.flowta.feature.integration.domain.repository.IntegrationLinkRepository
import com.j4x.flowta.feature.integration.domain.usecase.CreateIncomeTypeUseCase
import com.j4x.flowta.feature.types.domain.usecase.GetTransactionTypesByCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class ConnectSourceViewModel @Inject constructor(
    private val getAccounts: GetAccountsUseCase,
    private val getTypesByCategory: GetTransactionTypesByCategoryUseCase,
    private val createIncomeType: CreateIncomeTypeUseCase,
    private val links: IntegrationLinkRepository,
    private val clock: Clock
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectSourceState())
    val state = _state.asStateFlow()

    private val _effects = Channel<ConnectSourceEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var initialized = false
    private var preselected = false

    fun onIntent(intent: ConnectSourceIntent) {
        when (intent) {
            is ConnectSourceIntent.Initialize -> initialize(intent.sourceApp)
            is ConnectSourceIntent.SelectType ->
                _state.update { it.copy(selectedTypeId = intent.id, creatingNewType = false) }
            ConnectSourceIntent.SelectNewType ->
                _state.update { it.copy(selectedTypeId = null, creatingNewType = true) }
            is ConnectSourceIntent.SetNewTypeName -> _state.update { it.copy(newTypeName = intent.text) }
            is ConnectSourceIntent.SelectCashAccount -> _state.update { it.copy(cashAccountId = intent.id) }
            is ConnectSourceIntent.SelectTransferAccount -> _state.update { it.copy(transferAccountId = intent.id) }
            ConnectSourceIntent.Connect -> connect()
        }
    }

    private fun initialize(sourceApp: String) {
        if (initialized) return
        initialized = true
        _state.update { it.copy(sourceApp = sourceApp) }
        viewModelScope.launch {
            val existing = try {
                links.get(sourceApp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
            combine(getAccounts(), getTypesByCategory("INCOME")) { accounts, types -> accounts to types }
                .collect { (accounts, types) ->
                    _state.update { s ->
                        val base = s.copy(loaded = true, accounts = accounts, incomeTypes = types)
                        if (preselected) base else preselect(base, existing)
                    }
                    preselected = true
                }
        }
    }

    private fun preselect(s: ConnectSourceState, link: IntegrationLink?): ConnectSourceState {
        val accounts = s.accounts
        fun existing(id: String?) = id?.takeIf { wanted -> accounts.any { it.id == wanted } }
        fun first(vararg types: AccountType): String? =
            types.firstNotNullOfOrNull { type -> accounts.firstOrNull { it.type == type }?.id }
        return s.copy(
            selectedTypeId = link?.incomeTypeId?.takeIf { id -> s.incomeTypes.any { it.id == id } },
            cashAccountId = existing(link?.cashAccountId) ?: first(AccountType.CASH),
            transferAccountId = existing(link?.transferAccountId)
                ?: first(AccountType.DIGITAL_WALLET, AccountType.BANK)
        )
    }

    private fun connect() {
        val s = _state.value
        if (!s.canConnect) return
        val source = s.sourceApp ?: return
        val cashId = s.cashAccountId ?: return
        val transferId = s.transferAccountId ?: return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                val typeId = if (s.creatingNewType) {
                    createIncomeType(s.newTypeName).getOrThrow()
                } else {
                    s.selectedTypeId ?: return@launch
                }
                links.save(IntegrationLink(source, typeId, cashId, transferId, clock.millis()))
                _effects.send(ConnectSourceEffect.Connected)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
                _effects.send(ConnectSourceEffect.ShowError("No se pudo conectar. Intenta de nuevo."))
            }
        }
    }
}
