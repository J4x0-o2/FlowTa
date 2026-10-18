package com.j4x.flowta.core.presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.theme.ExpenseBubble
import com.j4x.flowta.core.presentation.theme.ExpenseRed
import com.j4x.flowta.core.presentation.theme.IncomeBubble
import com.j4x.flowta.core.presentation.theme.IncomeGreen
import com.j4x.flowta.core.presentation.theme.Indigo200
import com.j4x.flowta.core.presentation.theme.TransferBubble
import com.j4x.flowta.core.presentation.format.Formatters

@Composable
fun TransactionBubble(
    amount: Double,
    flowType: String,
    typeName: String,
    createdAt: Long = 0L,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val currencySymbol = LocalCurrencySymbol.current
    val currencyCode   = LocalCurrencyCode.current

    val isIncome   = flowType == "INCOME"
    val isTransfer = flowType == "TRANSFER"

    val bubbleColor = when {
        isTransfer -> TransferBubble
        isIncome   -> IncomeBubble
        else       -> ExpenseBubble
    }
    val amountColor = when {
        isTransfer -> Indigo200
        isIncome   -> IncomeGreen
        else       -> ExpenseRed
    }
    val alignment = when {
        isTransfer -> Arrangement.Center
        isIncome   -> Arrangement.End
        else       -> Arrangement.Start
    }
    val shape = when {
        isTransfer -> RoundedCornerShape(16.dp)
        isIncome   -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
        else       -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }
    val icon = when {
        isTransfer -> Icons.Default.SwapHoriz
        isIncome   -> Icons.AutoMirrored.Filled.CallReceived
        else       -> Icons.AutoMirrored.Filled.CallMade
    }
    val sign = when {
        isTransfer -> ""
        isIncome   -> "+"
        else       -> "-"
    }
    val columnAlignment = when {
        isTransfer -> Alignment.CenterHorizontally
        isIncome   -> Alignment.End
        else       -> Alignment.Start
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale = animateFloatAsState(
        targetValue   = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label         = "bubblePressScale"
    )

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = alignment
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer {
                    // Read inside the layer lambda so the press animation skips recomposition.
                    val s = pressScale.value
                    scaleX = s
                    scaleY = s
                }
                .widthIn(max = 260.dp)
                .clip(shape)
                .background(bubbleColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication        = null,
                    onClick           = onClick
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(horizontalAlignment = columnAlignment) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = icon,
                        contentDescription = null,
                        tint               = amountColor,
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text       = "$sign$currencySymbol${amount.toAmountString(currencyCode)}",
                        style      = MaterialTheme.typography.titleMedium,
                        color      = amountColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (createdAt > 0L) {
                    Text(
                        text  = createdAt.toTransactionDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun Double.toAmountString(code: CurrencyCode): String = Formatters.amount(this, code)

private fun Long.toTransactionDate(): String = Formatters.bubbleDate(this)
