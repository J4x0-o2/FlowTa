package com.j4x.flowta.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.format.Formatters
import com.j4x.flowta.core.presentation.theme.LocalDarkTheme
import com.j4x.flowta.core.presentation.theme.bubbleColors

/**
 * Collapsed streak of same-type movements. Same side and colors as [TransactionBubble],
 * with a second, offset layer behind it so it reads as a stack.
 */
@Composable
fun GroupedBubble(
    count: Int,
    total: Double,
    flowType: String,
    categories: List<String>,
    fromMillis: Long,
    toMillis: Long,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val currencySymbol = LocalCurrencySymbol.current
    val currencyCode   = LocalCurrencyCode.current

    val isIncome   = flowType == "INCOME"
    val isTransfer = flowType == "TRANSFER"

    val colors = bubbleColors(flowType, LocalDarkTheme.current)
    val arrangement = when {
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
    // Shift the back layer toward the inner side of the screen so it peeks out of the bubble.
    val layerShift = if (isIncome) (-6).dp else 6.dp
    val amountText = "$sign$currencySymbol${Formatters.amount(total, currencyCode)}"

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = arrangement
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .padding(start = if (isIncome) 6.dp else 0.dp, end = if (isIncome) 0.dp else 6.dp, bottom = 6.dp)
                .semantics {
                    contentDescription = "$count movimientos, $amountText. " +
                        if (expanded) "Tocá para contraer" else "Tocá para expandir"
                }
        ) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = layerShift, y = 6.dp)
                    .clip(shape)
                    .background(colors.container.copy(alpha = 0.55f))
            )
            Box(
                Modifier
                    .clip(shape)
                    .background(colors.container)
                    .clickable(role = Role.Button, onClick = onClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(horizontalAlignment = columnAlignment) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, null, tint = colors.content, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text       = amountText,
                            style      = MaterialTheme.typography.titleMedium,
                            color      = colors.content,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colors.content.copy(alpha = 0.18f))
                                .padding(horizontal = 7.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text       = "×$count",
                                style      = MaterialTheme.typography.labelMedium,
                                color      = colors.content,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (expanded) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ExpandLess, null, tint = colors.content, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text(
                        text     = categories.toCategorySummary(),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text  = Formatters.bubbleRange(fromMillis, toMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.date
                    )
                }
            }
        }
    }
}

/** "Mercado · Transporte · Café", or the first two plus "+N más" when there are many. */
internal fun List<String>.toCategorySummary(maxShown: Int = 3): String =
    if (size <= maxShown) joinToString(" · ")
    else take(maxShown - 1).joinToString(" · ") + " · +${size - (maxShown - 1)} más"
