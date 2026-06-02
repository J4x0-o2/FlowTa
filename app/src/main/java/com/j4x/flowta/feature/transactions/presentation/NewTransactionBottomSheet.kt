package com.j4x.flowta.feature.transactions.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.j4x.flowta.core.presentation.theme.ExpenseRed
import com.j4x.flowta.core.presentation.theme.Indigo500
import com.j4x.flowta.core.presentation.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewTransactionBottomSheet(
    state: AddTransactionState,
    onIntent: (AddTransactionIntent) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Nueva Transacción",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TransactionTab.entries.forEach { tab ->
                    TabButton(
                        tab = tab,
                        isActive = tab == state.activeTab,
                        onClick = { onIntent(AddTransactionIntent.SelectTab(tab)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Monto",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = { onIntent(AddTransactionIntent.SetAmount(it)) },
                    placeholder = {
                        Text("0.00", style = MaterialTheme.typography.headlineMedium)
                    },
                    textStyle = MaterialTheme.typography.headlineMedium,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            when (state.activeTab) {
                TransactionTab.INCOME -> IncomeTabContent(state, onIntent)
                TransactionTab.EXPENSE -> ExpenseTabContent(state, onIntent)
                TransactionTab.TRANSFER -> TransferTabContent(state, onIntent)
            }

            Button(
                onClick = { onIntent(AddTransactionIntent.Submit) },
                enabled = isFormValid(state) && !state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo500)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registrar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeTabContent(state: AddTransactionState, onIntent: (AddTransactionIntent) -> Unit) {
    FormDropdown(
        label = "Categoría",
        placeholder = "Seleccionar categoría",
        selectedName = state.incomeTypes.find { it.id == state.selectedTypeId }?.name,
        items = state.incomeTypes.map { it.id to it.name },
        onSelect = { onIntent(AddTransactionIntent.SelectType(it)) }
    )
    FormDropdown(
        label = "Fuente",
        placeholder = "¿Cómo recibiste?",
        selectedName = state.incomeSourceTypes.find { it.id == state.selectedSourceTypeId }?.name,
        items = state.incomeSourceTypes.map { it.id to it.name },
        onSelect = { onIntent(AddTransactionIntent.SelectSourceType(it)) }
    )
    FormDropdown(
        label = "Cuenta",
        placeholder = "Seleccionar cuenta",
        selectedName = state.accounts.find { it.id == state.selectedAccountId }?.name,
        items = state.accounts.map { it.id to it.name },
        onSelect = { onIntent(AddTransactionIntent.SelectAccount(it)) }
    )
    NoteField(note = state.note, onNoteChange = { onIntent(AddTransactionIntent.SetNote(it)) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseTabContent(state: AddTransactionState, onIntent: (AddTransactionIntent) -> Unit) {
    FormDropdown(
        label = "Categoría",
        placeholder = "Seleccionar categoría",
        selectedName = state.expenseTypes.find { it.id == state.selectedTypeId }?.name,
        items = state.expenseTypes.map { it.id to it.name },
        onSelect = { onIntent(AddTransactionIntent.SelectType(it)) }
    )
    FormDropdown(
        label = "Cuenta",
        placeholder = "Seleccionar cuenta",
        selectedName = state.accounts.find { it.id == state.selectedAccountId }?.name,
        items = state.accounts.map { it.id to it.name },
        onSelect = { onIntent(AddTransactionIntent.SelectAccount(it)) }
    )
    if (state.showNoteField) {
        NoteField(note = state.note, onNoteChange = { onIntent(AddTransactionIntent.SetNote(it)) })
    } else {
        Row(
            modifier = Modifier
                .clickable { onIntent(AddTransactionIntent.ToggleNoteField) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                "Agregar nota",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferTabContent(state: AddTransactionState, onIntent: (AddTransactionIntent) -> Unit) {
    FormDropdown(
        label = "Categoría",
        placeholder = "Seleccionar categoría",
        selectedName = state.transferTypes.find { it.id == state.selectedTypeId }?.name,
        items = state.transferTypes.map { it.id to it.name },
        onSelect = { onIntent(AddTransactionIntent.SelectType(it)) }
    )
    FormDropdown(
        label = "Desde cuenta",
        placeholder = "Seleccionar cuenta",
        selectedName = state.accounts.find { it.id == state.selectedFromAccountId }?.name,
        items = state.accounts.map { it.id to it.name },
        onSelect = { onIntent(AddTransactionIntent.SelectFromAccount(it)) }
    )
    val availableToAccounts = state.accounts.filter { it.id != state.selectedFromAccountId }
    FormDropdown(
        label = "Hacia cuenta",
        placeholder = "Cuenta destino",
        selectedName = availableToAccounts.find { it.id == state.selectedToAccountId }?.name,
        items = availableToAccounts.map { it.id to it.name },
        onSelect = { onIntent(AddTransactionIntent.SelectToAccount(it)) }
    )
    NoteField(note = state.note, onNoteChange = { onIntent(AddTransactionIntent.SetNote(it)) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormDropdown(
    label: String,
    placeholder: String,
    selectedName: String?,
    items: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedName ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(placeholder) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { (id, name) ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = { onSelect(id); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteField(note: String, onNoteChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "Nota",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            placeholder = { Text("Escribe una nota...") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 4
        )
    }
}

@Composable
private fun TabButton(
    tab: TransactionTab,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = when (tab) {
        TransactionTab.INCOME -> IncomeGreen
        TransactionTab.EXPENSE -> ExpenseRed
        TransactionTab.TRANSFER -> Indigo500
    }
    val icon = when (tab) {
        TransactionTab.INCOME -> Icons.AutoMirrored.Filled.CallReceived
        TransactionTab.EXPENSE -> Icons.AutoMirrored.Filled.CallMade
        TransactionTab.TRANSFER -> Icons.Default.SyncAlt
    }
    val label = when (tab) {
        TransactionTab.INCOME -> "Ingreso"
        TransactionTab.EXPENSE -> "Gasto"
        TransactionTab.TRANSFER -> "Transferencia"
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (isActive) color else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
    }
}

private fun isFormValid(state: AddTransactionState): Boolean {
    val amount = state.amountText.toDoubleOrNull() ?: return false
    if (amount <= 0) return false
    return when (state.activeTab) {
        TransactionTab.INCOME -> state.selectedTypeId != null && state.selectedAccountId != null
        TransactionTab.EXPENSE -> state.selectedTypeId != null && state.selectedAccountId != null
        TransactionTab.TRANSFER -> state.selectedFromAccountId != null && state.selectedToAccountId != null
    }
}
