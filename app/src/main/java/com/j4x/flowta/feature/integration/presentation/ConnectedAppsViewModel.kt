package com.j4x.flowta.feature.integration.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.accounts.domain.usecase.GetAccountsUseCase
import com.j4x.flowta.feature.integration.domain.repository.IntegrationLinkRepository
import com.j4x.flowta.feature.types.domain.usecase.GetTransactionTypesByCategoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the "Apps conectadas" card in Profile. Kept separate from [com.j4x.flowta.feature.profile.presentation.ProfileViewModel]
 * so the card owns its own state, effects and tests without touching Profile's account/capital wiring.
 */
@HiltViewModel
class ConnectedAppsViewModel @Inject constructor(
    private val links: IntegrationLinkRepository,
    getAccounts: GetAccountsUseCase,
    getTypesByCategory: GetTransactionTypesByCategoryUseCase,
    private val labels: AppLabelResolver
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectedAppsState())
    val state = _state.asStateFlow()

    private val _effects = Channel<ConnectedAppsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        combine(links.observeAll(), getAccounts(), getTypesByCategory("INCOME")) { all, accounts, types ->
            val accountNames = accounts.associate { it.id to it.name }
            val typeNames = types.associate { it.id to it.name }
            all.map { link ->
                ConnectedApp(
                    sourceApp = link.sourceApp,
                    label = labels.label(link.sourceApp),
                    typeName = typeNames[link.incomeTypeId],
                    cashAccountName = accountNames[link.cashAccountId],
                    transferAccountName = accountNames[link.transferAccountId]
                )
            }
        }
            .onEach { apps -> _state.update { it.copy(apps = apps) } }
            .catch { _effects.send(ConnectedAppsEffect.ShowError("No se pudieron cargar las apps conectadas")) }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: ConnectedAppsIntent) {
        when (intent) {
            is ConnectedAppsIntent.RequestDisconnect -> _state.update { s ->
                s.copy(pendingDisconnect = s.apps.firstOrNull { it.sourceApp == intent.sourceApp })
            }
            ConnectedAppsIntent.DismissDisconnect -> _state.update { it.copy(pendingDisconnect = null) }
            ConnectedAppsIntent.ConfirmDisconnect -> confirmDisconnect()
        }
    }

    private fun confirmDisconnect() {
        val target = _state.value.pendingDisconnect ?: return
        _state.update { it.copy(pendingDisconnect = null) }
        viewModelScope.launch {
            try {
                links.delete(target.sourceApp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _effects.send(ConnectedAppsEffect.ShowError("No se pudo desconectar. Intenta de nuevo."))
            }
        }
    }
}
