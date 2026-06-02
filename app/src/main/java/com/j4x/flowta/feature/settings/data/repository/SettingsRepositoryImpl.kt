package com.j4x.flowta.feature.settings.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.j4x.flowta.feature.settings.domain.model.AppTheme
import com.j4x.flowta.feature.settings.domain.model.UserPreferences
import com.j4x.flowta.feature.settings.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flowta_settings")

class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private val themeKey    = stringPreferencesKey("theme")
    private val currencyKey = stringPreferencesKey("currency")
    private val languageKey = stringPreferencesKey("language")

    override fun getPreferences(): Flow<UserPreferences> =
        context.dataStore.data.map { prefs ->
            UserPreferences(
                theme        = AppTheme.valueOf(prefs[themeKey] ?: AppTheme.SYSTEM.name),
                currencyCode = prefs[currencyKey] ?: "COP",
                language     = prefs[languageKey] ?: "es"
            )
        }

    override suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[themeKey] = theme }
    }

    override suspend fun setCurrency(code: String) {
        context.dataStore.edit { it[currencyKey] = code }
    }

    override suspend fun setLanguage(language: String) {
        context.dataStore.edit { it[languageKey] = language }
    }
}
