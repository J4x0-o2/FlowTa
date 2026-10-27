package com.j4x.flowta.core.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColors = darkColorScheme(
    primary          = Indigo200,
    onPrimary        = DarkBackground,
    primaryContainer = Indigo700,
    background       = DarkBackground,
    surface          = DarkSurface,
    surfaceVariant   = DarkSurface2,
)

private val LightColors = lightColorScheme(
    primary          = Indigo500,
    onPrimary        = LightBackground,
    primaryContainer = Indigo200,
    background       = LightBackground,
    surface          = LightSurface,
    surfaceVariant   = LightSurface2,
)

@Composable
fun FlowTaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else      -> LightColors
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = FlowTaTypography,
            content     = content
        )
    }
}
