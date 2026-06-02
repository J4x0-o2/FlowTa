package com.j4x.flowta.feature.transactions.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.components.BottomInputBar
import com.j4x.flowta.core.presentation.components.TransactionBubble
import com.j4x.flowta.core.presentation.navigation.Screen
import com.j4x.flowta.core.presentation.theme.ExpenseRed
import com.j4x.flowta.core.presentation.theme.IncomeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionScreen(
    navController: NavController,
    viewModel: TransactionViewModel = hiltViewModel(),
    addViewModel: AddTransactionViewModel = hiltViewModel()
) {
    val state           by viewModel.state.collectAsStateWithLifecycle()
    val addState        by addViewModel.state.collectAsStateWithLifecycle()
    val currencySymbol   = LocalCurrencySymbol.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                TransactionEffect.NavigateToAddForm      -> showAddSheet = true
                is TransactionEffect.ShowError           -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    LaunchedEffect(Unit) {
        addViewModel.effects.collect { effect ->
            when (effect) {
                AddTransactionEffect.Dismiss -> {
                    showAddSheet = false
                    addViewModel.onIntent(AddTransactionIntent.Reset)
                }
                is AddTransactionEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
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
                            text       = "FlowTa",
                            style      = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text  = "Balance: ${state.balance.toBalanceString(currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = when {
                                state.balance > 0 -> IncomeGreen
                                state.balance < 0 -> ExpenseRed
                                else              -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Metrics.route) }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Métricas")
                    }
                    IconButton(onClick = { navController.navigate(Screen.Profile.route) }) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                }
            )
        },
        bottomBar = {
            BottomInputBar(onAddTransaction = { viewModel.onIntent(TransactionIntent.OpenAddForm) })
        }
    ) { padding ->
        if (state.transactions.isEmpty()) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Registrá tu primera transacción",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(
                modifier      = Modifier.fillMaxSize().padding(padding),
                reverseLayout = true
            ) {
                items(state.transactions, key = { it.id }) { transaction ->
                    TransactionBubble(
                        amount    = transaction.amount,
                        flowType  = transaction.flowType,
                        typeName  = transaction.typeName,
                        createdAt = transaction.createdAt,
                        onClick   = { viewModel.onIntent(TransactionIntent.SelectTransaction(transaction.id)) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        NewTransactionBottomSheet(
            state     = addState,
            onIntent  = addViewModel::onIntent,
            onDismiss = {
                showAddSheet = false
                addViewModel.onIntent(AddTransactionIntent.Reset)
            }
        )
    }

    val selectedTransaction = state.selectedTransactionId
        ?.let { id -> state.transactions.find { it.id == id } }

    if (selectedTransaction != null) {
        TransactionDetailBottomSheet(
            transaction = selectedTransaction,
            onDismiss   = { viewModel.onIntent(TransactionIntent.DismissDetail) }
        )
    }
}

private fun Double.toBalanceString(symbol: String): String {
    val abs = kotlin.math.abs(this)
    val formatted = if (abs == abs.toLong().toDouble()) "${abs.toLong()}" else "%.2f".format(abs)
    return if (this < 0) "-$symbol$formatted" else "$symbol$formatted"
}
