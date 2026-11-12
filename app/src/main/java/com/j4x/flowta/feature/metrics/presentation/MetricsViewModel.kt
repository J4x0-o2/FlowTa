package com.j4x.flowta.feature.metrics.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.core.domain.time.DayTicker
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriod
import com.j4x.flowta.feature.metrics.domain.model.MetricsPeriodType
import com.j4x.flowta.core.presentation.theme.metricsColors
import com.j4x.flowta.feature.metrics.domain.usecase.ObserveColorOrderUseCase
import com.j4x.flowta.feature.metrics.domain.usecase.ObserveMetricsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Calendar-period Metrics. The selected tab and how many periods back the user is (0 = the current
 * one) live in [SavedStateHandle] so they survive process death. Counting back from the current period
 * (instead of storing a date) keeps "Hoy" / "Esta semana" current after midnight.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val observeMetrics: ObserveMetricsUseCase,
    observeColorOrder: ObserveColorOrderUseCase,
    private val ticker: DayTicker
) : ViewModel() {

    private val selectedType = savedState.getStateFlow(KEY_TYPE, DEFAULT_TYPE.name)
    private val periodsBack = savedState.getStateFlow(KEY_OFFSET, 0)
    private val earliest: StateFlow<Long?> =
        observeMetrics.earliest().stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val pickedDay = MutableStateFlow<LocalDate?>(null)

    /** Re-subscribes on every local day change so "today" never goes stale while the screen stays open. */
    val state: StateFlow<MetricsState> = ticker.days()
        .flatMapLatest { today ->
            combine(selectedType, periodsBack, ::Pair).flatMapLatest { (typeName, back) ->
                val type = parseType(typeName)
                val period = MetricsPeriod.containing(type, today).shift(-back)
                combine(observeMetrics(period, ticker.zone, today), pickedDay, observeColorOrder()) { metrics, picked, order ->
                    MetricsState(
                        periodType = type,
                        period = period,
                        today = today,
                        metrics = metrics,
                        selectedDay = picked?.takeIf { it in period } ?: today.takeIf { it in period },
                        canGoBack = metrics.hasPrevious,
                        canGoForward = !period.isCurrent(today),
                        colors = metricsColors(order.expenseIds, order.sourceIds)
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MetricsState())

    fun onIntent(intent: MetricsIntent) {
        when (intent) {
            is MetricsIntent.SelectType -> {
                pickedDay.value = null
                savedState[KEY_OFFSET] = 0
                savedState[KEY_TYPE] = intent.type.name
            }
            MetricsIntent.Previous -> if (hasPeriodBefore()) move(+1)
            MetricsIntent.Next -> if (currentOffset() > 0) move(-1)
            is MetricsIntent.SelectDay ->
                if (state.value.period?.contains(intent.date) == true) pickedDay.value = intent.date
        }
    }

    private fun currentOffset() = savedState.get<Int>(KEY_OFFSET) ?: 0

    /**
     * Computed from the saved offset and the earliest movement (not from the last emitted UI state),
     * so rapid taps can never step past the oldest period.
     */
    private fun hasPeriodBefore(): Boolean {
        val oldest = earliest.value ?: return false
        val type = parseType(savedState.get<String>(KEY_TYPE) ?: DEFAULT_TYPE.name)
        val shown = MetricsPeriod.containing(type, ticker.today()).shift(-currentOffset())
        return Instant.ofEpochMilli(oldest).atZone(ticker.zone).toLocalDate() < shown.start
    }

    private fun move(delta: Int) {
        pickedDay.value = null
        savedState[KEY_OFFSET] = (currentOffset() + delta).coerceAtLeast(0)
    }

    private fun parseType(name: String): MetricsPeriodType =
        MetricsPeriodType.entries.firstOrNull { it.name == name } ?: DEFAULT_TYPE

    private companion object {
        const val KEY_TYPE = "type"
        const val KEY_OFFSET = "offset"
        const val STOP_TIMEOUT_MS = 5_000L
        val DEFAULT_TYPE = MetricsPeriodType.DAY
    }
}
