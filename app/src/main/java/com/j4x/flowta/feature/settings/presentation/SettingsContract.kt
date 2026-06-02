package com.j4x.flowta.feature.settings.presentation

import com.j4x.flowta.feature.settings.domain.model.UserPreferences

data class SettingsState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false
)

sealed interface SettingsIntent {
    data class SetTheme(val theme: String) : SettingsIntent
    data class SetCurrency(val code: String) : SettingsIntent
}
