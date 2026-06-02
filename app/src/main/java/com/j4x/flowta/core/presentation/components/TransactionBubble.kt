package com.j4x.flowta.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.theme.ExpenseBubble
import com.j4x.flowta.core.presentation.theme.ExpenseRed
import com.j4x.flowta.core.presentation.theme.IncomeBubble
import com.j4x.flowta.core.presentation.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val isIncome   = flowType == "INCOME"
    val bubbleColor = if (isIncome) IncomeBubble else ExpenseBubble
    val amountColor = if (isIncome) IncomeGreen  else ExpenseRed
    val alignment   = if (isIncome) Arrangement.End else Arrangement.Start
    val cornerStart = if (isIncome) 16.dp else 4.dp
    val cornerEnd   = if (isIncome) 4.dp  else 16.dp

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = alignment
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = cornerStart, bottomEnd = cornerEnd))
                .background(bubbleColor)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(horizontalAlignment = if (isIncome) Alignment.End else Alignment.Start) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isIncome) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${if (isIncome) "+" else "-"}$currencySymbol${amount.toDisplayAmount()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = amountColor,
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

private fun Double.toDisplayAmount(): String =
    if (this == this.toLong().toDouble()) "${this.toLong()}" else "%.2f".format(this)

private fun Long.toTransactionDate(): String {
    val dayMonth = SimpleDateFormat("d MMM", Locale("es")).format(Date(this))
    val time     = SimpleDateFormat("h:mm a", Locale.ENGLISH).format(Date(this))
    return "$dayMonth · $time"
}
