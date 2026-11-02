package com.j4x.flowta.feature.integration.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.j4x.flowta.R
import com.j4x.flowta.feature.accounts.presentation.displayName

@Composable
fun ConnectSourceScreen(
    sourceLabel: String,
    viewModel: ConnectSourceViewModel,
    onConnected: () -> Unit,
    onCancel: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ConnectSourceEffect.Connected -> onConnected()
                is ConnectSourceEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.connect_title, sourceLabel),
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    stringResource(R.string.connect_explanation, sourceLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (state.noAccounts) {
                Text(
                    stringResource(R.string.connect_no_accounts),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                IncomeTypeField(state, viewModel::onIntent)
                AccountField(
                    label = stringResource(R.string.connect_cash_account),
                    state = state,
                    selectedId = state.cashAccountId,
                    onSelect = { viewModel.onIntent(ConnectSourceIntent.SelectCashAccount(it)) }
                )
                AccountField(
                    label = stringResource(R.string.connect_transfer_account),
                    state = state,
                    selectedId = state.transferAccountId,
                    onSelect = { viewModel.onIntent(ConnectSourceIntent.SelectTransferAccount(it)) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !state.isSaving,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.connect_cancel)) }
                Button(
                    onClick = { viewModel.onIntent(ConnectSourceIntent.Connect) },
                    enabled = state.canConnect,
                    modifier = Modifier.weight(1f)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.connect_confirm))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IncomeTypeField(state: ConnectSourceState, onIntent: (ConnectSourceIntent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val newTypeLabel = stringResource(R.string.connect_new_type_option)
    val shown = if (state.creatingNewType) newTypeLabel
    else state.incomeTypes.firstOrNull { it.id == state.selectedTypeId }?.name
        ?: stringResource(R.string.connect_select)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.connect_income_type), style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = shown,
                onValueChange = {},
                readOnly = true,
                enabled = !state.isSaving,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.incomeTypes.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name) },
                        onClick = { onIntent(ConnectSourceIntent.SelectType(type.id)); expanded = false }
                    )
                }
                DropdownMenuItem(
                    text = { Text(newTypeLabel) },
                    onClick = { onIntent(ConnectSourceIntent.SelectNewType); expanded = false }
                )
            }
        }
        if (state.creatingNewType) {
            OutlinedTextField(
                value = state.newTypeName,
                onValueChange = { onIntent(ConnectSourceIntent.SetNewTypeName(it)) },
                label = { Text(stringResource(R.string.connect_new_type_name)) },
                enabled = !state.isSaving,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountField(
    label: String,
    state: ConnectSourceState,
    selectedId: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val shown = state.accounts.firstOrNull { it.id == selectedId }?.name
        ?: stringResource(R.string.connect_select)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = shown,
                onValueChange = {},
                readOnly = true,
                enabled = !state.isSaving,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.accounts.forEach { account ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(account.name)
                                Text(
                                    "  ·  ${account.type.displayName()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = { onSelect(account.id); expanded = false }
                    )
                }
            }
        }
    }
}
