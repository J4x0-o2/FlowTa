package com.j4x.flowta.feature.transactions.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.components.BottomInputBar
import com.j4x.flowta.core.presentation.components.GroupedBubble
import com.j4x.flowta.core.presentation.components.TransactionBubble
import com.j4x.flowta.core.presentation.navigation.Screen
import com.j4x.flowta.core.presentation.theme.flowColors
import com.j4x.flowta.core.presentation.format.Formatters

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
    val currencyCode     = LocalCurrencyCode.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddSheet by remember { mutableStateOf(false) }
    // Expansion is UI state keyed by the group's stable key (oldest movement id).
    var expandedGroups by rememberSaveable { mutableStateOf(listOf<String>()) }
    val entries = remember(state.feed) { groupFeed(state.feed) }

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
                        val balanceColor by animateColorAsState(
                            targetValue = when {
                                state.balance > 0 -> flowColors.incomeText
                                state.balance < 0 -> flowColors.expenseText
                                else              -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            },
                            animationSpec = tween(200),
                            label         = "balanceColor"
                        )
                        Text(
                            text  = "Balance: ${state.balance.toBalanceString(currencySymbol, currencyCode)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = balanceColor
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.History.route) }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Historial")
                    }
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
        if (state.feed.isEmpty()) {
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
                items(entries, key = { it.key }, contentType = { it::class }) { entry ->
                    when (entry) {
                        is FeedEntry.Single -> FeedBubble(
                            item     = entry.item,
                            modifier = Modifier.animateItem(),
                            onSelect = { viewModel.onIntent(TransactionIntent.SelectTransaction(it)) }
                        )
                        is FeedEntry.Group -> {
                            val expanded = entry.key in expandedGroups
                            Column(Modifier.animateItem().animateContentSize()) {
                                GroupedBubble(
                                    count      = entry.count,
                                    total      = entry.total,
                                    flowType   = entry.flowType,
                                    categories = entry.categories,
                                    fromMillis = entry.fromMillis,
                                    toMillis   = entry.toMillis,
                                    expanded   = expanded,
                                    onClick    = {
                                        expandedGroups =
                                            if (expanded) expandedGroups - entry.key else expandedGroups + entry.key
                                    }
                                )
                                if (expanded) {
                                    // Chronological top to bottom, like the rest of the chat-style feed.
                                    entry.items.asReversed().forEach { item ->
                                        key(item.id) {
                                            FeedBubble(
                                                item     = item,
                                                onSelect = { viewModel.onIntent(TransactionIntent.SelectTransaction(it)) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
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

    val selectedTransaction = remember(state.selectedTransactionId, state.feed) {
        state.selectedTransactionId
            ?.let { id -> state.feed.filterIsInstance<FeedItem.Tx>().find { it.transaction.id == id }?.transaction }
    }

    if (selectedTransaction != null) {
        TransactionDetailBottomSheet(
            transaction = selectedTransaction,
            onDismiss   = { viewModel.onIntent(TransactionIntent.DismissDetail) }
        )
    }
}

private fun Double.toBalanceString(symbol: String, code: CurrencyCode): String {
    val abs = kotlin.math.abs(this)
    val num = Formatters.amount(abs, code)
    return if (this < 0) "-$symbol$num" else "$symbol$num"
}

@Composable
private fun FeedBubble(
    item: FeedItem,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    when (item) {
        is FeedItem.Tx -> TransactionBubble(
            amount    = item.transaction.amount,
            flowType  = item.transaction.flowType,
            typeName  = item.transaction.typeName,
            createdAt = item.transaction.createdAt,
            modifier  = modifier,
            onClick   = { onSelect(item.transaction.id) }
        )
        is FeedItem.Xfer -> TransactionBubble(
            amount    = item.transfer.amount,
            flowType  = "TRANSFER",
            typeName  = item.categoryLabel,
            createdAt = item.transfer.createdAt,
            modifier  = modifier,
            onClick   = {}
        )
    }
}
