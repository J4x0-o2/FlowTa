package com.j4x.flowta.feature.transactions.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.theme.ExpenseRed
import com.j4x.flowta.core.presentation.theme.Indigo500
import com.j4x.flowta.core.presentation.theme.IncomeGreen
import com.j4x.flowta.feature.transactions.domain.model.Transaction
import com.j4x.flowta.core.presentation.format.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
    transaction: Transaction,
    onDismiss: () -> Unit
) {
    val currencySymbol = LocalCurrencySymbol.current
    val currencyCode   = LocalCurrencyCode.current
    val isIncome  = transaction.flowType == "INCOME"
    val flowColor = if (isIncome) IncomeGreen else ExpenseRed
    val flowIcon  = if (isIncome) Icons.AutoMirrored.Filled.CallReceived else Icons.AutoMirrored.Filled.CallMade
    val flowLabel = if (isIncome) "Ingreso" else "Gasto"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tipo badge
            Surface(
                shape = RoundedCornerShape(50),
                color = flowColor.copy(alpha = 0.15f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = flowIcon,
                        contentDescription = null,
                        tint = flowColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text       = flowLabel,
                        style      = MaterialTheme.typography.labelMedium,
                        color      = flowColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Monto
            Text(
                text       = "${if (isIncome) "+" else "-"}$currencySymbol${transaction.amount.toDisplayAmount(currencyCode)}",
                style      = MaterialTheme.typography.headlineLarge,
                color      = flowColor,
                fontWeight = FontWeight.Bold
            )

            // Filas de detalle
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow(
                    icon  = Icons.Default.Label,
                    label = "Categoría",
                    value = transaction.typeName
                )
                if (!transaction.sourceTypeName.isNullOrBlank()) {
                    DetailRow(
                        icon  = Icons.Default.Input,
                        label = "Fuente",
                        value = transaction.sourceTypeName
                    )
                }
                DetailRow(
                    icon  = Icons.Default.AccountBalance,
                    label = "Cuenta",
                    value = transaction.accountName
                )
                DetailRow(
                    icon  = Icons.Default.CalendarToday,
                    label = "Fecha",
                    value = transaction.createdAt.toDetailDate()
                )
                if (!transaction.note.isNullOrBlank()) {
                    DetailRow(
                        icon  = Icons.AutoMirrored.Filled.Note,
                        label = "Nota",
                        value = transaction.note
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            TextButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Cerrar",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text  = value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

private fun Double.toDisplayAmount(code: CurrencyCode): String = Formatters.amount(this, code)

private fun Long.toDetailDate(): String = Formatters.detailDate(this)
