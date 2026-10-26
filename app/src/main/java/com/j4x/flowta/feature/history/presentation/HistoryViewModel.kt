package com.j4x.flowta.feature.history.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.history.domain.usecase.ObserveMonthHistoryUseCase
import com.j4x.flowta.feature.history.domain.usecase.ObserveYearHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

/** Year screen. The selected year lives in [SavedStateHandle] so it survives process death. */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    observeYear: ObserveYearHistoryUseCase
) : ViewModel() {

    private val selectedYear = savedState.getStateFlow(KEY_YEAR, LocalDate.now().year)

    val state: StateFlow<HistoryState> = selectedYear
        .flatMapLatest { observeYear(it) }
        .map { HistoryState(YearHistoryModel.from(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryState())

    fun onIntent(intent: HistoryIntent) {
        val shown = state.value.year ?: return
        when (intent) {
            HistoryIntent.PreviousYear -> if (shown.canGoBack) savedState[KEY_YEAR] = shown.year - 1
            HistoryIntent.NextYear -> if (shown.canGoForward) savedState[KEY_YEAR] = shown.year + 1
        }
    }

    private companion object {
        const val KEY_YEAR = "year"
    }
}

/** Month screen. Year and month come from the navigation arguments via [SavedStateHandle]. */
@HiltViewModel
class HistoryMonthViewModel @Inject constructor(
    savedState: SavedStateHandle,
    observeMonth: ObserveMonthHistoryUseCase
) : ViewModel() {

    private val year: Int = checkNotNull(savedState["year"])
    private val month: Int = checkNotNull(savedState["month"])

    val state: StateFlow<HistoryMonthState> = observeMonth(year, month)
        .map { HistoryMonthState(MonthHistoryModel.from(it)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HistoryMonthState())
}

private const val STOP_TIMEOUT_MS = 5_000L
