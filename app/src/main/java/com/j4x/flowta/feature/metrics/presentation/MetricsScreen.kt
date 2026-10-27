package com.j4x.flowta.feature.metrics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.automirrored.filled.TrendingDown
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.j4x.flowta.core.presentation.theme.Indigo500
import com.j4x.flowta.core.domain.model.CurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.theme.flowColors
import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.DailyFlow
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Fill
import com.j4x.flowta.core.presentation.format.Formatters

private val chartPalette = listOf(
    Color(0xFFE74C3C), Color(0xFF3498DB), Color(0xFF3A9564),
    Color(0xFFF39C12), Color(0xFF9B59B6), Color(0xFF1ABC9C),
    Color(0xFFE67E22), Color(0xFF34495E), Color(0xFF95A5A6)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricsScreen(
    navController: NavController,
    viewModel: MetricsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Métricas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tabs Semana / Mes / Todo
            item {
                RangeTabs(
                    selected = state.selectedRange,
                    onSelect = { viewModel.onIntent(MetricsIntent.SelectRange(it)) }
                )
            }

            // Tarjetas resumen 2×2
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryCard(
                            label  = "Ingresos",
                            amount = state.totalIncome,
                            color  = flowColors.incomeText,
                            icon   = Icons.AutoMirrored.Filled.CallReceived,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            label  = "Gastos",
                            amount = state.totalExpense,
                            color  = flowColors.expenseText,
                            icon   = Icons.AutoMirrored.Filled.CallMade,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SummaryCard(
                            label  = "Transferencias",
                            amount = state.totalTransfers,
                            color  = Indigo500,
                            icon   = Icons.Default.SwapHoriz,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryCard(
                            label  = "Flujo neto",
                            amount = state.netFlow,
                            color  = when {
                                state.netFlow > 0 -> flowColors.incomeText
                                state.netFlow < 0 -> flowColors.expenseText
                                else              -> MaterialTheme.colorScheme.onSurface
                            },
                            icon   = Icons.AutoMirrored.Filled.TrendingDown,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Gráfico Ingresos vs Gastos
            item {
                MetricsCard(title = "Ingresos vs Gastos") {
                    IncomeExpenseBarChart(dailyFlows = state.dailyFlows)
                }
            }

            // Gráfico Gastos por categoría
            item {
                MetricsCard(title = "Gastos por categoría") {
                    ExpensesDonutChart(categories = state.expensesByCategory)
                }
            }
        }
    }
}

@Composable
private fun RangeTabs(
    selected: DateRangeOption,
    onSelect: (DateRangeOption) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            DateRangeOption.entries.forEach { option ->
                val isActive = option == selected
                Surface(
                    onClick = { onSelect(option) },
                    shape = RoundedCornerShape(50),
                    color = if (isActive) Indigo500 else Color.Transparent
                ) {
                    Text(
                        text  = option.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isActive) Color.White
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    label: String,
    amount: Double,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val symbol = LocalCurrencySymbol.current
    val code   = LocalCurrencyCode.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
            )
            Text(
                text       = amount.toDisplayAmount(symbol, code),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = color
            )
        }
    }
}

@Composable
private fun MetricsCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
private fun IncomeExpenseBarChart(dailyFlows: List<DailyFlow>) {
    if (dailyFlows.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sin datos en este período",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        return
    }

    val chart    = remember(dailyFlows) { computeBarChartData(dailyFlows) }
    val days     = chart.days
    val maxValue = chart.maxValue

    val incomeFill  = flowColors.incomeFill
    val expenseFill = flowColors.expenseFill

    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        val groupW  = size.width / days.size
        val barW    = (groupW * 0.32f).coerceAtLeast(4f)
        val gap     = 2f

        days.forEachIndexed { i, day ->
            val income  = day.income
            val expense = day.expense
            val cx      = (i + 0.5f) * groupW

            if (income > 0) {
                val h = income / maxValue * size.height
                drawRect(
                    color    = incomeFill,
                    topLeft  = Offset(cx - barW - gap, size.height - h),
                    size     = Size(barW, h),
                    style    = Fill
                )
            }
            if (expense > 0) {
                val h = expense / maxValue * size.height
                drawRect(
                    color    = expenseFill,
                    topLeft  = Offset(cx + gap, size.height - h),
                    size     = Size(barW, h),
                    style    = Fill
                )
            }
        }
    }

    // Leyenda
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(color = flowColors.incomeFill, label = "Ingresos")
        Spacer(Modifier.width(16.dp))
        LegendDot(color = flowColors.expenseFill, label = "Gastos")
    }
}

@Composable
private fun ExpensesDonutChart(categories: List<CategoryExpense>) {
    val donut   = remember(categories) { computeDonutData(categories) }
    val nonZero = donut.slices
    val total   = donut.total

    if (total == 0.0) {
        Box(
            modifier = Modifier.fillMaxWidth().height(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Sin gastos en este período",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
        return
    }

    val holeColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            nonZero.forEachIndexed { i, cat ->
                val sweep = (cat.total / total * 360f).toFloat()
                drawArc(
                    color      = chartPalette[i % chartPalette.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter  = true,
                    style      = Fill
                )
                startAngle += sweep
            }
            drawCircle(color = holeColor, radius = size.minDimension * 0.3f)
        }

        Spacer(Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            nonZero.take(7).forEachIndexed { i, cat ->
                val pct = (cat.total / total * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(chartPalette[i % chartPalette.size])
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${cat.typeName} $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private fun Double.toDisplayAmount(symbol: String, code: CurrencyCode): String {
    val sign = if (this < 0) "-" else ""
    val abs  = kotlin.math.abs(this)
    val num = Formatters.amount(abs, code)
    return "$sign$symbol$num"
}
