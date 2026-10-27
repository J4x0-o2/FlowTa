package com.j4x.flowta.core.presentation.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/** True when [FlowTaTheme] is rendering its dark scheme (honors the manual Settings toggle). */
val LocalDarkTheme = compositionLocalOf { false }

data class BubbleColors(val container: Color, val content: Color, val date: Color)

fun bubbleColors(flowType: String, dark: Boolean): BubbleColors = when (flowType) {
    "TRANSFER" ->
        if (dark) BubbleColors(TransferBubbleDark, TransferBubbleTextDark, TransferBubbleDateDark)
        else BubbleColors(TransferBubbleLight, TransferBubbleTextLight, TransferBubbleDateLight)
    "INCOME" ->
        if (dark) BubbleColors(IncomeBubbleDark, IncomeBubbleTextDark, IncomeBubbleDateDark)
        else BubbleColors(IncomeBubbleLight, IncomeBubbleTextLight, IncomeBubbleDateLight)
    else ->
        if (dark) BubbleColors(ExpenseBubbleDark, ExpenseBubbleTextDark, ExpenseBubbleDateDark)
        else BubbleColors(ExpenseBubbleLight, ExpenseBubbleTextLight, ExpenseBubbleDateLight)
}
