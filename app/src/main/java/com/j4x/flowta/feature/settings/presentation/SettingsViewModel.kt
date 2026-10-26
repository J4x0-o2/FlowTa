package com.j4x.flowta.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.settings.domain.repository.SettingsRepository
import com.j4x.flowta.feature.settings.domain.usecase.ResetAllDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val resetAllData: ResetAllDataUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state = _state.asStateFlow()

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        repository.getPreferences()
            .onEach { prefs -> _state.update { it.copy(preferences = prefs) } }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.SetTheme    -> viewModelScope.launch { repository.setTheme(intent.theme) }
            is SettingsIntent.SetCurrency -> viewModelScope.launch { repository.setCurrency(intent.code) }
            SettingsIntent.OpenResetAll ->
                _state.update { it.copy(showResetAll = true, resetConfirmation = "", resetError = null) }
            SettingsIntent.DismissResetAll -> dismissResetAll()
            is SettingsIntent.ResetConfirmationChanged ->
                _state.update { it.copy(resetConfirmation = intent.text) }
            SettingsIntent.ConfirmResetAll -> confirmResetAll()
        }
    }

    private fun dismissResetAll() {
        if (_state.value.isResetting) return
        _state.update { it.copy(showResetAll = false, resetConfirmation = "", resetError = null) }
    }

    private fun confirmResetAll() {
        if (!_state.value.canConfirmResetAll) return
        _state.update { it.copy(isResetting = true, resetError = null) }
        viewModelScope.launch {
            resetAllData()
                .onSuccess {
                    _state.update {
                        it.copy(showResetAll = false, resetConfirmation = "", isResetting = false)
                    }
                    _effects.send(SettingsEffect.DataReset)
                }
                .onFailure { e ->
                    _state.update { it.copy(isResetting = false, resetError = e.message ?: "Error") }
                }
        }
    }
}
