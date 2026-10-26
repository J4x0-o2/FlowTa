package com.j4x.flowta.feature.settings.domain.repository

import com.j4x.flowta.feature.settings.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getPreferences(): Flow<UserPreferences>
    suspend fun setTheme(theme: String)
    suspend fun setCurrency(code: String)
    suspend fun setLanguage(language: String)
    /** Removes every stored preference so theme, currency and language return to their defaults. */
    suspend fun clear()
}
