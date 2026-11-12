package com.j4x.flowta.feature.metrics.domain.usecase

import com.j4x.flowta.feature.history.domain.repository.HistoryRepository
import com.j4x.flowta.feature.metrics.domain.model.DateRange
import com.j4x.flowta.feature.metrics.domain.model.MetricsAggregator
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriod
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType
import com.j4x.flowta.feature.metrics.domain.model.PeriodMetrics
import com.j4x.flowta.feature.metrics.domain.repository.MetricsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Streams the metrics of one calendar [MetricsPeriod]. Movements come from the history queries and are
 * grouped by local date in Kotlin; the previous period is fetched together so deltas need no extra query.
 */
class ObserveMetricsUseCase @Inject constructor(
    private val history: HistoryRepository,
    private val metrics: MetricsRepository
) {
    operator fun invoke(
        period: MetricsPeriod,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone)
    ): Flow<PeriodMetrics> {
        val range = period.range(zone)
        val previousFrom = period.previous().range(zone).from
        val categories = if (period.type == MetricsPeriodType.DAY) {
            flowOf(emptyList())
        } else {
            // The category query is inclusive on both ends, the period range is [from, to).
            metrics.getExpensesByCategory(DateRange(range.from, range.to - 1))
        }
        return combine(
            history.observeMovements(previousFrom, range.to),
            history.observeEarliest(),
            categories
        ) { movements, earliest, byCategory ->
            MetricsAggregator.compute(period, movements, byCategory, earliest, today, zone)
        }
    }
}
