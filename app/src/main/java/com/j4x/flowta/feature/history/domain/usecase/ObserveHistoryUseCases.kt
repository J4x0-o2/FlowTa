package com.j4x.flowta.feature.history.domain.usecase

import com.j4x.flowta.feature.history.domain.model.HistoryAggregator
import com.j4x.flowta.feature.history.domain.model.MonthDetail
import com.j4x.flowta.feature.history.domain.model.YearHistory
import com.j4x.flowta.feature.history.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** Local midnight at the start of [date] in [zone], as epoch millis: range bounds follow the user's calendar, not UTC. */
private fun startMillis(date: LocalDate, zone: ZoneId): Long =
    date.atStartOfDay(zone).toInstant().toEpochMilli()

/** Streams the history of a calendar year: its totals and its months with movements. */
class ObserveYearHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    operator fun invoke(
        year: Int,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone)
    ): Flow<YearHistory> = combine(
        repository.observeMovements(startMillis(LocalDate.of(year, 1, 1), zone), startMillis(LocalDate.of(year + 1, 1, 1), zone)),
        repository.observeEarliest()
    ) { movements, earliest -> HistoryAggregator.year(year, movements, earliest, today, zone) }
}

/** Streams the totals and weeks of one month. */
class ObserveMonthHistoryUseCase @Inject constructor(
    private val repository: HistoryRepository
) {
    operator fun invoke(
        year: Int,
        month: Int,
        zone: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zone)
    ): Flow<MonthDetail> {
        val selected = YearMonth.of(year, month)
        return repository
            .observeMovements(startMillis(selected.atDay(1), zone), startMillis(selected.plusMonths(1).atDay(1), zone))
            .map { movements -> HistoryAggregator.month(selected, movements, today, zone) }
    }
}
