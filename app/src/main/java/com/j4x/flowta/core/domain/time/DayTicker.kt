package com.j4x.flowta.core.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transformLatest
import java.time.Duration
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Today's local date, as a value and as a stream that moves on at every local midnight. */
class DayTicker @Inject constructor(
    private val clock: Clock,
    private val timeChanges: TimeChangeSignal
) {
    constructor(clock: Clock) : this(clock, TimeChangeSignal.None)


    val zone get() = clock.zone

    fun today(): LocalDate = LocalDate.now(clock)

    /**
     * Emits today right away and the new date each time the local day changes. A time zone / clock
     * change signal restarts the wait, so the date and the next-midnight timer follow the new zone.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun days(): Flow<LocalDate> = timeChanges.changes
        .onStart { emit(Unit) }
        .transformLatest {
            while (true) {
                val now = clock.instant()
                val today = LocalDate.ofInstant(now, clock.zone)
                emit(today)
                val nextMidnight = today.plusDays(1).atStartOfDay(clock.zone).toInstant()
                delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1))
            }
        }
        .distinctUntilChanged()
}
