package com.j4x.flowta.feature.metrics.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.theme.Indigo500
import com.j4x.flowta.core.presentation.theme.MetricsColors
import com.j4x.flowta.core.presentation.theme.flowColors
import com.j4x.flowta.feature.history.presentation.formatExpense
import com.j4x.flowta.feature.history.presentation.formatIncome
import com.j4x.flowta.feature.history.presentation.formatMoney
import com.j4x.flowta.feature.metrics.domain.model.DayTotals
import com.j4x.flowta.feature.metrics.domain.model.MetricsAggregator
import com.j4x.flowta.feature.metrics.domain.model.PercentChange
import com.j4x.flowta.feature.metrics.domain.model.PeriodMetrics
import java.time.LocalDate

private const val TOP_CATEGORIES = 5
private val ChartHeight = 120.dp

/** Cards of the Week tab: net hero, income vs expenses per day, expenses by category, quick stats. */
internal fun LazyListScope.weekMetricsItems(
    metrics: PeriodMetrics,
    colors: MetricsColors,
    isCurrentWeek: Boolean,
    selectedDay: LocalDate?,
    onSelectDay: (LocalDate) -> Unit
) {
    item { WeekHeroCard(metrics, isCurrentWeek) }
    item { DailyBarsCard(metrics.days, selectedDay, onSelectDay) }
    item { CategoriesCard(metrics, colors) }
    item { WeekStatsRow(metrics) }
}

@Composable
private fun WeekHeroCard(metrics: PeriodMetrics, isCurrentWeek: Boolean) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    MetricsCard(title = "Neto de la semana") {
        Text(
            text = formatNet(metrics.net, symbol, code),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = netColor(metrics.net)
        )
        metrics.netChange?.let { change ->
            val tone = changeTone(change, goodWhenUp = true)
            Surface(shape = RoundedCornerShape(50), color = tone.copy(alpha = 0.15f)) {
                Text(
                    text = "${change.arrowText()} vs. ${if (isCurrentWeek) "semana pasada" else "semana anterior"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = tone,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowTile(
                label = "Ingresos",
                amount = formatIncome(metrics.income, symbol, code),
                color = flowColors.incomeText,
                change = metrics.incomeChange,
                goodWhenUp = true,
                modifier = Modifier.weight(1f)
            )
            FlowTile(
                label = "Gastos",
                amount = formatExpense(metrics.expense, symbol, code),
                color = flowColors.expenseText,
                change = metrics.expenseChange,
                goodWhenUp = false,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FlowTile(
    label: String,
    amount: String,
    color: Color,
    change: PercentChange?,
    goodWhenUp: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = mutedText())
            Text(
                amount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            change?.let {
                Text(
                    text = "${it.arrowText()} vs. anterior",
                    style = MaterialTheme.typography.labelSmall,
                    color = changeTone(it, goodWhenUp)
                )
            }
        }
    }
}

@Composable
internal fun changeTone(change: PercentChange, goodWhenUp: Boolean): Color = when {
    change.percent == 0 -> mutedText()
    (change.percent > 0) == goodWhenUp -> flowColors.incomeText
    else -> flowColors.expenseText
}

@Composable
internal fun mutedText(): Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

@Composable
private fun DailyBarsCard(days: List<DayTotals>, selectedDay: LocalDate?, onSelectDay: (LocalDate) -> Unit) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    val maxValue = remember(days) { days.maxOfOrNull { maxOf(it.income, it.expense) } ?: 0.0 }
    val selected = days.firstOrNull { it.date == selectedDay }

    MetricsCard(title = "Ingresos vs. gastos por día") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = flowColors.incomeFill, label = "Ingr.")
            Spacer(Modifier.width(12.dp))
            LegendDot(color = flowColors.expenseFill, label = "Gast.")
            Spacer(Modifier.weight(1f))
            if (maxValue > 0) {
                Text("máx. ${formatMoney(maxValue, symbol, code)}", style = MaterialTheme.typography.labelSmall, color = mutedText())
            }
        }

        // Fixed height so the chart does not jump when a day is (de)selected.
        Box(modifier = Modifier.fillMaxWidth().height(28.dp), contentAlignment = Alignment.Center) {
            Text(
                text = selected?.let {
                    "${shortDayLabel(it.date)} · ${formatIncome(it.income, symbol, code)} · ${formatExpense(it.expense, symbol, code)}"
                } ?: "Toca un día para ver el detalle",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected != null) FontWeight.Bold else FontWeight.Normal,
                color = if (selected != null) MaterialTheme.colorScheme.onSurface else mutedText(),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        Box(modifier = Modifier.fillMaxWidth().height(ChartHeight + 28.dp)) {
            Canvas(modifier = Modifier.fillMaxWidth().height(ChartHeight)) {
                listOf(0f, 0.5f, 1f).forEach { fraction ->
                    val y = size.height * fraction
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                days.forEach { day ->
                    DayColumn(
                        day = day,
                        maxValue = maxValue,
                        isSelected = day.date == selectedDay,
                        onClick = { onSelectDay(day.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayColumn(
    day: DayTotals,
    maxValue: Double,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val incomeFill = flowColors.incomeFill
    val expenseFill = flowColors.expenseFill
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    val description = "${shortDayLabel(day.date)}, ingresos ${formatMoney(day.income, symbol, code)}, " +
        "gastos ${formatMoney(day.expense, symbol, code)}"

    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Indigo500.copy(alpha = 0.15f) else Color.Transparent)
            .semantics(mergeDescendants = true) {
                contentDescription = description
                selected = isSelected
            }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(ChartHeight)) {
            if (maxValue <= 0.0) return@Canvas
            val barWidth = size.width * 0.28f
            val gap = 1.5.dp.toPx()
            val minHeight = 2.dp.toPx()
            val center = size.width / 2f

            fun bar(value: Double, left: Float, color: Color) {
                if (value <= 0.0) return
                val h = (value / maxValue * size.height).toFloat().coerceAtLeast(minHeight)
                drawRect(color, Offset(left, size.height - h), Size(barWidth, h))
            }
            bar(day.income, center - gap - barWidth, incomeFill)
            bar(day.expense, center + gap, expenseFill)
        }
        Text(
            text = weekdayInitial(day.date),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onSurface else mutedText(),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun CategoriesCard(metrics: PeriodMetrics, colors: MetricsColors) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    val shares = remember(metrics.categories) { MetricsAggregator.categoryShares(metrics.categories) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    val visible = if (expanded) shares else shares.take(TOP_CATEGORIES)

    MetricsCard(title = "Gastos por categoría") {
        if (shares.isEmpty()) {
            Text("Sin gastos en este período", style = MaterialTheme.typography.bodySmall, color = mutedText())
            return@MetricsCard
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            visible.forEach { share ->
                val color = colors.category(share.category.typeId)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            share.category.typeName,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatMoney(share.category.total, symbol, code),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "  ${share.percent}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedText()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color.copy(alpha = 0.18f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(share.barFraction)
                                .fillMaxHeight()
                                .background(color)
                        )
                    }
                }
            }
        }
        if (shares.size > TOP_CATEGORIES) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Ver menos" else "Ver todas (${shares.size})")
                }
            }
        }
    }
}

@Composable
private fun WeekStatsRow(metrics: PeriodMetrics) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    val priciest = metrics.mostExpensiveDay
    val savings = metrics.savingsPercent

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            title = "Promedio diario",
            value = formatMoney(metrics.averageDailyExpense, symbol, code),
            caption = "de gasto",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Día más caro",
            value = priciest?.let { weekdayName(it.date) } ?: "—",
            caption = priciest?.let { formatMoney(it.expense, symbol, code) } ?: "sin gastos",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Ahorro",
            value = savings?.let { "$it%" } ?: "—",
            caption = "de ingresos",
            valueColor = if (savings != null && savings < 0) flowColors.expenseText else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = mutedText(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(caption, style = MaterialTheme.typography.labelSmall, color = mutedText(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
