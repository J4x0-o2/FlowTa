package com.j4x.flowta.core.presentation

import androidx.compose.runtime.compositionLocalOf
import com.j4x.flowta.core.domain.model.CurrencyCode

val LocalCurrencySymbol = compositionLocalOf { "$" }
val LocalCurrencyCode   = compositionLocalOf { CurrencyCode.USD }
