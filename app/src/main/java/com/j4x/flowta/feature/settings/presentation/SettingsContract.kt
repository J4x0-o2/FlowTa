package com.j4x.flowta.feature.settings.presentation

import com.j4x.flowta.feature.settings.domain.model.UserPreferences

/** The exact word the user must type to unlock the "erase everything" confirmation. */
const val RESET_CONFIRMATION_WORD = "BORRAR"

data class SettingsState(
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val showResetAll: Boolean = false,
    val resetConfirmation: String = "",
    val isResetting: Boolean = false,
    val resetError: String? = null
) {
    val canConfirmResetAll: Boolean
        get() = resetConfirmation == RESET_CONFIRMATION_WORD && !isResetting
}

sealed interface SettingsIntent {
    data class SetTheme(val theme: String) : SettingsIntent
    data class SetCurrency(val code: String) : SettingsIntent
    data object OpenResetAll : SettingsIntent
    data object DismissResetAll : SettingsIntent
    data class ResetConfirmationChanged(val text: String) : SettingsIntent
    data object ConfirmResetAll : SettingsIntent
}

sealed interface SettingsEffect {
    /** All data was erased; the screen must navigate to the start destination clearing the back stack. */
    data object DataReset : SettingsEffect
}
