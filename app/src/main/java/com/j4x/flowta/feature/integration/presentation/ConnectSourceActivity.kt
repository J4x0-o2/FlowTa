package com.j4x.flowta.feature.integration.presentation

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.theme.FlowTaTheme
import com.j4x.flowta.feature.settings.domain.model.AppTheme
import com.j4x.flowta.feature.settings.presentation.SettingsViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Connect handshake screen. Started for result by a source app that holds the RECORD_INCOME
 * permission; the caller is identified by [getCallingPackage], never by an extra.
 */
@AndroidEntryPoint
class ConnectSourceActivity : ComponentActivity() {

    private val viewModel: ConnectSourceViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = callingPackage
        if (source == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        viewModel.onIntent(ConnectSourceIntent.Initialize(source))
        val sourceLabel = appLabel(source)

        enableEdgeToEdge()
        setContent {
            val settings by settingsViewModel.state.collectAsStateWithLifecycle()
            val isDark = when (settings.preferences.theme) {
                AppTheme.DARK   -> true
                AppTheme.LIGHT  -> false
                AppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            val currencyCode = CurrencyCode.entries
                .find { it.displayCode == settings.preferences.currencyCode } ?: CurrencyCode.USD
            FlowTaTheme(darkTheme = isDark) {
                CompositionLocalProvider(
                    LocalCurrencySymbol provides currencyCode.symbol,
                    LocalCurrencyCode   provides currencyCode
                ) {
                    ConnectSourceScreen(
                        sourceLabel = sourceLabel,
                        viewModel = viewModel,
                        onConnected = {
                            setResult(Activity.RESULT_OK)
                            finish()
                        },
                        onCancel = {
                            setResult(Activity.RESULT_CANCELED)
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun appLabel(packageName: String): String = try {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        packageName
    }
}
