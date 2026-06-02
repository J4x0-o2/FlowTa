package com.j4x.flowta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.navigation.NavGraph
import com.j4x.flowta.core.presentation.theme.FlowTaTheme
import com.j4x.flowta.feature.settings.domain.model.AppTheme
import com.j4x.flowta.feature.settings.presentation.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by settingsViewModel.state.collectAsState()
            val isDark = when (state.preferences.theme) {
                AppTheme.DARK   -> true
                AppTheme.LIGHT  -> false
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            val currencySymbol = CurrencyCode.entries
                .find { it.displayCode == state.preferences.currencyCode }?.symbol ?: "$"
            FlowTaTheme(darkTheme = isDark) {
                CompositionLocalProvider(LocalCurrencySymbol provides currencySymbol) {
                    NavGraph()
                }
            }
        }
    }
}
