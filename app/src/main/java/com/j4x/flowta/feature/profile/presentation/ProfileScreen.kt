package com.j4x.flowta.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.navigation.Screen
import com.j4x.flowta.core.presentation.theme.ExpenseRed
import com.j4x.flowta.core.presentation.theme.Indigo500
import com.j4x.flowta.core.presentation.theme.IncomeGreen
import com.j4x.flowta.feature.accounts.presentation.AccountCard
import com.j4x.flowta.feature.accounts.presentation.AccountEffect
import com.j4x.flowta.feature.accounts.presentation.AccountIntent
import com.j4x.flowta.feature.accounts.presentation.AccountViewModel
import com.j4x.flowta.feature.accounts.presentation.NewAccountBottomSheet
import com.j4x.flowta.feature.settings.presentation.SettingsBottomSheet
import com.j4x.flowta.feature.types.domain.model.TransactionType
import com.j4x.flowta.feature.types.presentation.TypeChip
import com.j4x.flowta.feature.types.presentation.TypesBottomSheet
import com.j4x.flowta.feature.types.presentation.TypesIntent
import com.j4x.flowta.feature.types.presentation.TypesViewModel
import com.j4x.flowta.core.presentation.format.Formatters

private enum class ActiveSheet { SETTINGS, NEW_ACCOUNT, INCOME_TYPES, EXPENSE_TYPES, TRANSFER_TYPES }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val accountViewModel: AccountViewModel = hiltViewModel()
    val typesViewModel: TypesViewModel = hiltViewModel()

    val accountState by accountViewModel.state.collectAsState()
    val typesState by typesViewModel.state.collectAsState()

    var activeSheet by remember { mutableStateOf<ActiveSheet?>(null) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        accountViewModel.effects.collect { effect ->
            when (effect) {
                is AccountEffect.NavigateToDetail ->
                    navController.navigate(Screen.AccountDetail().createRoute(effect.accountId))
                is AccountEffect.ShowError ->
                    snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Perfil") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { activeSheet = ActiveSheet.SETTINGS }) {
                        Icon(Icons.Default.Settings, contentDescription = "Configuración")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Capital Total
            item {
                CapitalTotalSection(
                    total = accountState.totalCapital,
                    accountCount = accountState.accounts.size
                )
            }

            // Mis Cuentas
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mis Cuentas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Surface(
                        onClick = { activeSheet = ActiveSheet.NEW_ACCOUNT },
                        shape = RoundedCornerShape(20.dp),
                        color = Indigo500
                    ) {
                        Text(
                            "+ Nueva",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            if (accountState.accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No tienes cuentas aún",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                items(accountState.accounts, key = { it.id }) { account ->
                    AccountCard(
                        account = account,
                        onClick = { accountViewModel.onIntent(AccountIntent.NavigateToDetail(account.id)) }
                    )
                }
            }

            // Categorías
            item {
                Text("Categorías", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                CategoriesCard(
                    incomeTypes   = typesState.incomeTypes,
                    expenseTypes  = typesState.expenseTypes,
                    transferTypes = typesState.transferTypes,
                    onAddIncome   = { activeSheet = ActiveSheet.INCOME_TYPES },
                    onAddExpense  = { activeSheet = ActiveSheet.EXPENSE_TYPES },
                    onAddTransfer = { activeSheet = ActiveSheet.TRANSFER_TYPES },
                    onDelete      = { typesViewModel.onIntent(TypesIntent.DeleteType(it)) }
                )
            }

            // Cerrar sesión
            item {
                Surface(
                    onClick = { showLogoutDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    color = ExpenseRed.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Cerrar sesión",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ExpenseRed
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Cerrar sesión") },
            text  = { Text("¿Confirmás que querés cerrar la sesión?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    navController.navigate(Screen.Transactions.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Cerrar sesión", color = ExpenseRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    when (activeSheet) {
        ActiveSheet.SETTINGS -> SettingsBottomSheet(onDismiss = { activeSheet = null })

        ActiveSheet.NEW_ACCOUNT -> NewAccountBottomSheet(
            onDismiss = { activeSheet = null },
            onConfirm = { name, type, balance, metaAmount ->
                accountViewModel.onIntent(
                    AccountIntent.AddAccount(
                        name            = name,
                        type            = type,
                        initialBalance  = balance,
                        metaAmount      = metaAmount,
                        metaPeriodicity = if (metaAmount != null) "monthly" else null
                    )
                )
                activeSheet = null
            }
        )

        ActiveSheet.INCOME_TYPES -> TypesBottomSheet(
            title     = "Tipos de Ingreso",
            category  = "INCOME",
            types     = typesState.incomeTypes,
            chipColor = IncomeGreen,
            onDismiss = { activeSheet = null },
            onAdd     = { name, cat -> typesViewModel.onIntent(TypesIntent.AddType(name, cat)) },
            onDelete  = { typesViewModel.onIntent(TypesIntent.DeleteType(it)) }
        )

        ActiveSheet.EXPENSE_TYPES -> TypesBottomSheet(
            title     = "Tipos de Gasto",
            category  = "EXPENSE",
            types     = typesState.expenseTypes,
            chipColor = ExpenseRed,
            onDismiss = { activeSheet = null },
            onAdd     = { name, cat -> typesViewModel.onIntent(TypesIntent.AddType(name, cat)) },
            onDelete  = { typesViewModel.onIntent(TypesIntent.DeleteType(it)) }
        )

        ActiveSheet.TRANSFER_TYPES -> TypesBottomSheet(
            title     = "Tipos de Transferencia",
            category  = "TRANSFER",
            types     = typesState.transferTypes,
            chipColor = Indigo500,
            onDismiss = { activeSheet = null },
            onAdd     = { name, cat -> typesViewModel.onIntent(TypesIntent.AddType(name, cat)) },
            onDelete  = { typesViewModel.onIntent(TypesIntent.DeleteType(it)) }
        )

        null -> Unit
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoriesCard(
    incomeTypes: List<TransactionType>,
    expenseTypes: List<TransactionType>,
    transferTypes: List<TransactionType>,
    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onDelete: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            CategorySection(
                title     = "Tipos de Ingreso",
                types     = incomeTypes,
                chipColor = IncomeGreen,
                onAdd     = onAddIncome,
                onDelete  = onDelete
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            CategorySection(
                title     = "Tipos de Gasto",
                types     = expenseTypes,
                chipColor = ExpenseRed,
                onAdd     = onAddExpense,
                onDelete  = onDelete
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            CategorySection(
                title     = "Tipos de Transferencia",
                types     = transferTypes,
                chipColor = Indigo500,
                onAdd     = onAddTransfer,
                onDelete  = onDelete
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    title: String,
    types: List<TransactionType>,
    chipColor: Color,
    onAdd: () -> Unit,
    onDelete: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onAdd, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Agregar tipo", tint = chipColor, modifier = Modifier.size(18.dp))
            }
        }
        if (types.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                types.forEach { type ->
                    TypeChip(name = type.name, color = chipColor, onDelete = { onDelete(type.id) })
                }
            }
        }
    }
}

@Composable
private fun CapitalTotalSection(total: Double, accountCount: Int) {
    val symbol = LocalCurrencySymbol.current
    val code   = LocalCurrencyCode.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = Indigo500,
            modifier = Modifier.size(40.dp)
        )
        Text(
            "Capital Total",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = total.formatCapital(symbol, code),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = when {
                total > 0 -> IncomeGreen
                total < 0 -> ExpenseRed
                else      -> MaterialTheme.colorScheme.onSurface
            }
        )
        Text(
            "$accountCount cuenta${if (accountCount != 1) "s" else ""}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun Double.formatCapital(symbol: String, code: CurrencyCode): String {
    val abs = kotlin.math.abs(this)
    val num = Formatters.amount(abs, code)
    return if (this < 0) "-$symbol$num" else "$symbol$num"
}
