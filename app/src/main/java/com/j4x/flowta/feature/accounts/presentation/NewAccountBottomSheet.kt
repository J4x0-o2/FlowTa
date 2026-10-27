package com.j4x.flowta.feature.accounts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.components.AmountInput
import com.j4x.flowta.core.presentation.format.parseAmount
import com.j4x.flowta.feature.accounts.domain.model.AccountType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewAccountBottomSheet(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, balance: Double, metaAmount: Double?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AccountType.SAVINGS) }
    var balanceText by remember { mutableStateOf("") }
    var metaText by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    val currencyCode = LocalCurrencyCode.current
    val showMeta = selectedType == AccountType.SAVINGS
    val balance = parseAmount(balanceText, currencyCode)
    val meta = parseAmount(metaText, currencyCode)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nueva Cuenta", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            // Nombre
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Nombre", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Mi cuenta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Tipo
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Tipo", style = MaterialTheme.typography.labelMedium)
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        AccountType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName()) },
                                onClick = { selectedType = type; typeExpanded = false }
                            )
                        }
                    }
                }
            }

            // Saldo inicial
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Saldo inicial", style = MaterialTheme.typography.labelMedium)
                AmountInput(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    previewLabel = "Saldo",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Meta (solo SAVINGS)
            if (showMeta) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Meta (opcional)", style = MaterialTheme.typography.labelMedium)
                    AmountInput(
                        value = metaText,
                        onValueChange = { metaText = it },
                        placeholder = "Monto objetivo",
                        previewLabel = "Meta",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Button(
                onClick = {
                    onConfirm(name.trim(), selectedType.name, balance ?: 0.0, meta?.takeIf { it > 0 })
                },
                enabled = name.trim().isNotEmpty() && balance != null && (!showMeta || meta != null),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Crear Cuenta")
            }
        }
    }
}

fun AccountType.displayName() = when (this) {
    AccountType.CASH           -> "Efectivo"
    AccountType.BANK           -> "Cuenta bancaria"
    AccountType.DIGITAL_WALLET -> "Billetera digital"
    AccountType.SAVINGS        -> "Ahorros"
    AccountType.OTHER          -> "Otro"
}
