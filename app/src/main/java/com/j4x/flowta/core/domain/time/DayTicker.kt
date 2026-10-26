package com.j4x.flowta.core.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Today's local date, as a value and as a stream that moves on at every local midnight. */
class DayTicker @Inject constructor(private val clock: Clock) {

    val zone get() = clock.zone

    fun today(): LocalDate = LocalDate.now(clock)

    /** Emits today right away and the new date each time the local day changes. */
    fun days(): Flow<LocalDate> = flow {
        while (true) {
            val now = clock.instant()
            val today = LocalDate.ofInstant(now, clock.zone)
            emit(today)
            val nextMidnight = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
            delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1))
        }
    }.distinctUntilChanged()
}
