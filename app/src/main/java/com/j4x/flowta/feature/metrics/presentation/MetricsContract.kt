package com.j4x.flowta.feature.metrics.presentation

import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriod
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType
import com.j4x.flowta.feature.metrics.domain.model.PeriodMetrics
import java.time.LocalDate

data class MetricsState(
    val periodType: MetricsPeriodType = MetricsPeriodType.DAY,
    val period: MetricsPeriod? = null,
    /** Local today the period was resolved against; titles and "en curso" depend on it. */
    val today: LocalDate? = null,
    val metrics: PeriodMetrics? = null,
    /** Highlighted day of the shown period (Week view); null when none. */
    val selectedDay: LocalDate? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
) {
    val isLoading: Boolean get() = metrics == null
}

sealed interface MetricsIntent {
    data class SelectType(val type: MetricsPeriodType) : MetricsIntent
    data object Previous : MetricsIntent
    data object Next : MetricsIntent
    data class SelectDay(val date: LocalDate) : MetricsIntent
}

sealed interface MetricsEffect
