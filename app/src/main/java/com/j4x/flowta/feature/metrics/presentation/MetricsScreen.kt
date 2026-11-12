package com.j4x.flowta.feature.metrics.presentation

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.theme.Indigo500
import com.j4x.flowta.core.presentation.theme.categoryColor
import com.j4x.flowta.core.presentation.theme.flowColors
import com.j4x.flowta.feature.metrics.domain.model.CategoryExpense
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType

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
            item {
                PeriodTabs(
                    selected = state.periodType,
                    onSelect = { viewModel.onIntent(MetricsIntent.SelectType(it)) }
                )
            }

            val period = state.period
            val today = state.today
            val metrics = state.metrics
            if (period != null && today != null) {
                item {
                    PeriodStepper(
                        title = periodTitle(period, today),
                        subtitle = periodSubtitle(period, today),
                        canGoBack = state.canGoBack,
                        canGoForward = state.canGoForward,
                        onPrevious = { viewModel.onIntent(MetricsIntent.Previous) },
                        onNext = { viewModel.onIntent(MetricsIntent.Next) }
                    )
                }
            }
            if (metrics != null) {
                when (state.periodType) {
                    // Month and Year only show their net until their dedicated views land.
                    MetricsPeriodType.DAY,
                    MetricsPeriodType.MONTH,
                    MetricsPeriodType.YEAR -> item { NetCard(state.periodType.netLabel(), metrics.net) }
                    MetricsPeriodType.WEEK -> weekMetricsItems(
                        metrics = metrics,
                        isCurrentWeek = period != null && today != null && period.isCurrent(today),
                        selectedDay = state.selectedDay,
                        onSelectDay = { viewModel.onIntent(MetricsIntent.SelectDay(it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodTabs(
    selected: MetricsPeriodType,
    onSelect: (MetricsPeriodType) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(modifier = Modifier.padding(4.dp)) {
            MetricsPeriodType.entries.forEach { type ->
                val isActive = type == selected
                Surface(
                    onClick = { onSelect(type) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    color = if (isActive) Indigo500 else Color.Transparent
                ) {
                    Text(
                        text = type.tabLabel(),
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isActive) Color.White
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PeriodStepper(
    title: String,
    subtitle: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = canGoBack) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Período anterior")
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Período siguiente")
        }
    }
}

/** The net of a period with its sign: green when positive, red when negative, neutral at zero. */
@Composable
internal fun NetCard(label: String, net: Double) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    MetricsCard(title = label) {
        Text(
            text = formatNet(net, symbol, code),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = netColor(net)
        )
    }
}

@Composable
internal fun netColor(net: Double): Color = when {
    net > 0 -> flowColors.incomeText
    net < 0 -> flowColors.expenseText
    else -> MaterialTheme.colorScheme.onSurface
}

@Composable
internal fun MetricsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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

/** Donut of expenses by category, colored by [categoryColor]. Kept for the Month and Year views. */
@Composable
internal fun ExpensesDonutChart(categories: List<CategoryExpense>) {
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
            nonZero.forEach { cat ->
                val sweep = (cat.total / total * 360f).toFloat()
                drawArc(
                    color      = categoryColor(cat.typeId),
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
            nonZero.take(7).forEach { cat ->
                val pct = (cat.total / total * 100).toInt()
                LegendDot(color = categoryColor(cat.typeId), label = "${cat.typeName} $pct%")
            }
        }
    }
}

@Composable
internal fun LegendDot(color: Color, label: String) {
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
