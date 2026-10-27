package com.j4x.flowta.feature.history.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.navigation.Screen
import com.j4x.flowta.core.presentation.theme.flowColors

/** Year screen: year stepper, year totals and one card per month with movements. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    HistoryScaffold(title = "Historial", subtitle = null, onBack = { navController.navigateUp() }) {
        state.year?.let { year ->
            item {
                YearStepper(
                    year = year,
                    onPrevious = { viewModel.onIntent(HistoryIntent.PreviousYear) },
                    onNext = { viewModel.onIntent(HistoryIntent.NextYear) }
                )
            }
            item {
                HeroCard(label = "Neto del año", net = year.net) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile("Ingresos", year.income, TileKind.INCOME, Modifier.weight(1f))
                        Tile("Gastos", year.expense, TileKind.EXPENSE, Modifier.weight(1f))
                    }
                }
            }
            if (year.isEmpty) {
                item { EmptyYear(year.year) }
            } else {
                item { SectionTitle("Por mes") }
                items(year.months, key = { it.month.toString() }) { row ->
                    MonthCard(row) {
                        navController.navigate(Screen.HistoryMonth().createRoute(row.month.year, row.month.monthValue))
                    }
                }
            }
        }
    }
}

/** Month screen: month totals with a card per week, newest first. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryMonthScreen(
    navController: NavController,
    viewModel: HistoryMonthViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val month = state.month

    HistoryScaffold(
        title = month?.title.orEmpty(),
        subtitle = month?.year?.toString(),
        onBack = { navController.navigateUp() }
    ) {
        month?.let {
            item {
                HeroCard(label = "Neto del mes", net = it.net) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tile("Ingresos", it.income, TileKind.INCOME, Modifier.weight(1f))
                        Tile("Gastos", it.expense, TileKind.EXPENSE, Modifier.weight(1f))
                        Tile("Movimientos", it.count.toDouble(), TileKind.COUNT, Modifier.weight(1f))
                    }
                }
            }
            item { SectionTitle("Por semana") }
            items(it.weeks, key = { week -> week.rangeText }) { week -> WeekCard(week) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScaffold(
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    content: LazyListScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (subtitle != null) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun YearStepper(year: YearHistoryModel, onPrevious: () -> Unit, onNext: () -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, enabled = year.canGoBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Año anterior")
            }
            Text(year.year.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = onNext, enabled = year.canGoForward) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Año siguiente")
            }
        }
    }
}

@Composable
private fun netColor(net: Double): Color = when {
    net > 0 -> flowColors.incomeText
    net < 0 -> flowColors.expenseText
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
private fun HeroCard(label: String, net: Double, tiles: @Composable () -> Unit) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = formatMoney(net, symbol, code),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = netColor(net),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            tiles()
        }
    }
}

private enum class TileKind { INCOME, EXPENSE, COUNT }

@Composable
private fun Tile(label: String, value: Double, kind: TileKind, modifier: Modifier = Modifier) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    val (text, color) = when (kind) {
        TileKind.INCOME -> formatIncome(value, symbol, code) to flowColors.incomeText
        TileKind.EXPENSE -> formatExpense(value, symbol, code) to flowColors.expenseText
        TileKind.COUNT -> value.toInt().toString() to MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun MonthCard(row: MonthRowModel, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeriodBody(
                title = if (row.isCurrent) "${row.name} · en curso" else row.name,
                income = row.income,
                expense = row.expense,
                net = row.net,
                incomeFraction = row.incomeFraction,
                expenseFraction = row.expenseFraction,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun WeekCard(week: WeekRowModel) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        PeriodBody(
            title = if (week.isCurrent) "${week.rangeText} · en curso" else week.rangeText,
            income = week.income,
            expense = week.expense,
            net = week.net,
            incomeFraction = week.incomeFraction,
            expenseFraction = week.expenseFraction,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}

/** Title with the net on the right, an income bar and an expense bar, and the "+ income · - expense" line. */
@Composable
private fun PeriodBody(
    title: String,
    income: Double,
    expense: Double,
    net: Double,
    incomeFraction: Float,
    expenseFraction: Float,
    modifier: Modifier = Modifier
) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )
            Text(
                formatMoney(net, symbol, code),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = netColor(net),
                maxLines = 1
            )
        }
        Bar(incomeFraction, flowColors.incomeFill)
        Bar(expenseFraction, flowColors.expenseFill)
        Text(
            "${formatIncome(income, symbol, code)} · ${formatExpense(expense, symbol, code)}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), CircleShape)
    ) {
        if (fraction > 0f) {
            Box(Modifier.fillMaxWidth(fraction).height(4.dp).background(color, CircleShape))
        }
    }
}

@Composable
private fun EmptyYear(year: Int) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Aún no hay movimientos en $year", style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        Text(
            "Cuando registres un ingreso o gasto, aparece aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}
