package com.j4x.flowta.feature.history.presentation

/** Year screen state; [year] is null until the first read. */
data class HistoryState(val year: YearHistoryModel? = null)

sealed interface HistoryIntent {
    data object PreviousYear : HistoryIntent
    data object NextYear : HistoryIntent
}

/** Month screen state; [month] is null until the first read. */
data class HistoryMonthState(val month: MonthHistoryModel? = null)
