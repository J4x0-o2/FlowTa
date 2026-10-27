package com.j4x.flowta.core.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/** Semantic income/expense colors, resolved from the app's real dark-theme flag. */
data class FlowColors(
    val incomeText: Color,
    val incomeFill: Color,
    val incomeSolid: Color,
    val expenseText: Color,
    val expenseFill: Color,
    val expenseSolid: Color,
    val onSolid: Color
)

fun flowColorsFor(dark: Boolean): FlowColors =
    if (dark) FlowColors(
        IncomeTextDark, IncomeFillDark, IncomeSolidDark,
        ExpenseTextDark, ExpenseFillDark, ExpenseSolidDark, OnSolidDark
    ) else FlowColors(
        IncomeTextLight, IncomeFillLight, IncomeSolidLight,
        ExpenseTextLight, ExpenseFillLight, ExpenseSolidLight, OnSolidLight
    )

val flowColors: FlowColors
    @Composable @ReadOnlyComposable get() = flowColorsFor(LocalDarkTheme.current)
