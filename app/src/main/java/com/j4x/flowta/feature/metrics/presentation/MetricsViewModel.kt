package com.j4x.flowta.feature.metrics.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.metrics.domain.model.DateRange
import com.j4x.flowta.feature.metrics.domain.repository.MetricsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MetricsViewModel @Inject constructor(
    private val repository: MetricsRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(DateRangeOption.MONTH)

    private val _state = MutableStateFlow(MetricsState())
    val state = _state.asStateFlow()

    init {
        _selectedRange
            .flatMapLatest { option ->
                val range = option.toDateRange()
                combine(
                    repository.getIncomeVsExpenseByDay(range),
                    repository.getExpensesByCategory(range),
                    repository.getTotalTransfers(range)
                ) { flows, categories, transfers ->
                    val income  = flows.filter { it.flowType == "INCOME"  }.sumOf { it.total }
                    val expense = flows.filter { it.flowType == "EXPENSE" }.sumOf { it.total }
                    MetricsSnapshot(option, flows, categories, transfers, income, expense)
                }
            }
            .onEach { snap ->
                _state.update {
                    it.copy(
                        selectedRange      = snap.range,
                        dailyFlows         = snap.dailyFlows,
                        expensesByCategory = snap.categories,
                        totalIncome        = snap.income,
                        totalExpense       = snap.expense,
                        totalTransfers     = snap.transfers,
                        netFlow            = snap.income - snap.expense
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: MetricsIntent) {
        when (intent) {
            is MetricsIntent.SelectRange -> _selectedRange.value = intent.range
        }
    }

    private fun DateRangeOption.toDateRange(): DateRange {
        val now = System.currentTimeMillis()
        return when (this) {
            DateRangeOption.WEEK  -> DateRange(now - 7L  * 86_400_000, now)
            DateRangeOption.MONTH -> DateRange(now - 30L * 86_400_000, now)
            DateRangeOption.ALL   -> DateRange(0L, Long.MAX_VALUE)
        }
    }

    private data class MetricsSnapshot(
        val range: DateRangeOption,
        val dailyFlows: List<com.j4x.flowta.feature.metrics.domain.model.DailyFlow>,
        val categories: List<com.j4x.flowta.feature.metrics.domain.model.CategoryExpense>,
        val transfers: Double,
        val income: Double,
        val expense: Double
    )
}
