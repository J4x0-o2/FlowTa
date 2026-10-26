package com.j4x.flowta.feature.accounts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.j4x.flowta.core.presentation.components.TransactionBubble
import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.theme.ExpenseRed
import com.j4x.flowta.core.presentation.theme.Indigo500
import com.j4x.flowta.core.presentation.theme.IncomeGreen
import com.j4x.flowta.feature.accounts.domain.model.AccountType
import com.j4x.flowta.feature.transactions.presentation.TransactionDetailBottomSheet
import com.j4x.flowta.core.presentation.format.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    navController: NavController,
    viewModel: AccountDetailViewModel = hiltViewModel()
) {
    val state          by viewModel.state.collectAsStateWithLifecycle()
    val account         = state.account
    val currencySymbol  = LocalCurrencySymbol.current
    val currencyCode    = LocalCurrencyCode.current

    val selectedTx = state.selectedTransactionId
        ?.let { id -> state.transactions.find { it.id == id } }

    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is AccountDetailEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            account?.name ?: "Cuenta",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (account != null) {
                            val balance = account.balance
                            Text(
                                text  = balance.toBalanceString(currencySymbol, currencyCode),
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    balance > 0 -> IncomeGreen
                                    balance < 0 -> ExpenseRed
                                    else        -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("Reiniciar cuenta") },
                            onClick = {
                                menuExpanded = false
                                viewModel.requestReset()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier      = Modifier.fillMaxSize().padding(padding),
            reverseLayout = true
        ) {
            // Progreso de meta (SAVINGS) — aparece al fondo en reverseLayout = arriba visualmente
            if (account?.type == AccountType.SAVINGS
                && account.metaAmount != null
                && account.metaAmount > 0
            ) {
                item(key = "savings_progress") {
                    SavingsProgressCard(
                        balance  = account.balance,
                        meta     = account.metaAmount,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            if (state.transactions.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Sin movimientos en esta cuenta",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                items(state.transactions, key = { it.id }, contentType = { "transaction" }) { transaction ->
                    TransactionBubble(
                        amount    = transaction.amount,
                        flowType  = transaction.flowType,
                        typeName  = transaction.typeName,
                        createdAt = transaction.createdAt,
                        modifier  = Modifier.animateItem(),
                        onClick        = { viewModel.selectTransaction(transaction.id) }
                    )
                }
            }
        }
    }

    state.resetDialog?.let { dialog ->
        ResetAccountDialogContent(
            accountName    = account?.name ?: "cuenta",
            movementCount  = dialog.movementCount,
            currencySymbol = currencySymbol,
            onConfirm      = viewModel::confirmReset,
            onDismiss      = viewModel::dismissReset
        )
    }

    if (selectedTx != null) {
        TransactionDetailBottomSheet(
            transaction = selectedTx,
            onDismiss   = { viewModel.dismissDetail() }
        )
    }
}

@Composable
private fun ResetAccountDialogContent(
    accountName: String,
    movementCount: Long,
    currencySymbol: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    val startingBalance = parseStartingBalance(amountText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Reiniciar $accountName?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Se borrarán $movementCount movimientos de esta cuenta " +
                        "(ingresos, gastos y transferencias). Esta acción no se puede deshacer."
                )
                OutlinedTextField(
                    value           = amountText,
                    onValueChange   = { amountText = it },
                    label           = { Text("Saldo inicial") },
                    placeholder     = { Text("0") },
                    prefix          = { Text(currencySymbol) },
                    isError         = startingBalance == null,
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier        = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick  = { startingBalance?.let(onConfirm) },
                enabled  = startingBalance != null
            ) {
                Text("Reiniciar", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun SavingsProgressCard(
    balance: Double,
    meta: Double,
    modifier: Modifier = Modifier
) {
    val progress       = (balance / meta).coerceIn(0.0, 1.0).toFloat()
    val currencySymbol = LocalCurrencySymbol.current
    val currencyCode   = LocalCurrencyCode.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Meta de ahorro",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    meta.toBalanceString(currencySymbol, currencyCode),
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress   = { progress },
                modifier   = Modifier.fillMaxWidth(),
                color      = Indigo500,
                trackColor = Indigo500.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(0.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${(progress * 100).toInt()}% completado",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    "monthly",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

private fun Double.toBalanceString(symbol: String, code: CurrencyCode): String {
    val abs = kotlin.math.abs(this)
    val num = Formatters.amount(abs, code)
    return if (this < 0) "-$symbol$num" else "$symbol$num"
}
