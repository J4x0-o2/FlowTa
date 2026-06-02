package com.j4x.flowta.feature.settings.domain.model

data class UserPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val currencyCode: String = "COP",
    val language: String = "es"
)

enum class AppTheme { LIGHT, DARK, SYSTEM }
