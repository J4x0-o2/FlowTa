package com.j4x.flowta.feature.metrics.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.j4x.flowta.core.presentation.LocalCurrencyCode
import com.j4x.flowta.core.presentation.LocalCurrencySymbol
import com.j4x.flowta.core.presentation.theme.flowColors
import com.j4x.flowta.core.presentation.theme.MetricsColors
import com.j4x.flowta.feature.history.presentation.formatMoney
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType
import com.j4x.flowta.feature.metrics.domain.model.PeriodMetrics

private val RingSize = 72.dp
private val RingStroke = 8.dp

/** Cards of the Month and Year tabs (same layout): net hero with savings ring, categories, income sources. */
internal fun LazyListScope.monthYearMetricsItems(type: MetricsPeriodType, metrics: PeriodMetrics, colors: MetricsColors) {
    val isYear = type == MetricsPeriodType.YEAR
    item { MonthYearHeroCard(type, metrics) }
    if (metrics.totals.count == 0) {
        item {
            Text(
                text = if (isYear) "Aún no hay movimientos en este año." else "Aún no hay movimientos en este mes.",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedText(),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                textAlign = TextAlign.Center
            )
        }
        return
    }
    item { CategoriesDonutCard(metrics, colors) }
    item { IncomeSourcesCard(metrics, colors) }
}

@Composable
private fun MonthYearHeroCard(type: MetricsPeriodType, metrics: PeriodMetrics) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    val incomeColor = flowColors.incomeText
    val expenseColor = flowColors.expenseText
    val ring = savingsRing(metrics.income, metrics.expense)

    MetricsCard(title = type.netLabel()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = formatNet(metrics.net, symbol, code),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = netColor(metrics.net),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildAnnotatedString {
                        append("Ingresos ")
                        withStyle(SpanStyle(color = incomeColor, fontWeight = FontWeight.SemiBold)) {
                            append(formatMoney(metrics.income, symbol, code))
                        }
                        append(" · Gastos ")
                        withStyle(SpanStyle(color = expenseColor, fontWeight = FontWeight.SemiBold)) {
                            append(formatMoney(metrics.expense, symbol, code))
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedText()
                )
                metrics.netChange?.let { change ->
                    val tone = changeTone(change, goodWhenUp = true)
                    Surface(shape = RoundedCornerShape(50), color = tone.copy(alpha = 0.15f)) {
                        Text(
                            text = "${change.arrowText()} vs. ${if (type == MetricsPeriodType.YEAR) "año anterior" else "mes anterior"}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = tone,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            SavingsRing(ring)
        }
    }
}

@Composable
private fun SavingsRing(state: SavingsRingState) {
    val track = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val progressColor = flowColors.incomeText
    val textColor = if (state.isNegative) flowColors.expenseText else MaterialTheme.colorScheme.onSurface
    Box(modifier = Modifier.size(RingSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(RingSize)) {
            val stroke = RingStroke.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
            if (state.progress > 0f) {
                drawArc(
                    progressColor, -90f, 360f * state.progress, false, Offset(inset, inset), arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(state.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1)
            Text("ahorro", style = MaterialTheme.typography.labelSmall, color = mutedText())
        }
    }
}

@Composable
private fun CategoriesDonutCard(metrics: PeriodMetrics, colors: MetricsColors) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    MetricsCard(title = "Gastos por categoría") {
        ExpensesDonutChart(
            categories = metrics.categories,
            colors = colors,
            centerText = formatCompactMoney(metrics.expense, symbol, code),
            centerCaption = "gastado"
        )
    }
}

@Composable
private fun IncomeSourcesCard(metrics: PeriodMetrics, colors: MetricsColors) {
    val symbol = LocalCurrencySymbol.current
    val code = LocalCurrencyCode.current
    val sources = metrics.incomeSources
    MetricsCard(title = "Ingresos por fuente") {
        if (sources.isEmpty()) {
            Text("Sin ingresos en este período", style = MaterialTheme.typography.bodySmall, color = mutedText())
            return@MetricsCard
        }
        Row(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))) {
            sources.barSegments().forEach { source ->
                Box(
                    modifier = Modifier
                        .weight(source.total.toFloat())
                        .height(12.dp)
                        .background(colors.incomeSource(source.id))
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            sources.forEach { source ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(colors.incomeSource(source.id))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        source.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        formatMoney(source.total, symbol, code),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("  ${source.percent}%", style = MaterialTheme.typography.bodySmall, color = mutedText())
                }
            }
        }
    }
}
