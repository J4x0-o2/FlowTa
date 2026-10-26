package com.j4x.flowta.core.domain.time

import kotlinx.coroutines.test.TestCoroutineScheduler
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/** A [Clock] that starts at [start] and moves with the virtual time of [scheduler]. */
class VirtualClock(
    private val scheduler: TestCoroutineScheduler,
    private val start: Instant,
    private val zone: ZoneId
) : Clock() {
    override fun getZone(): ZoneId = zone
    override fun withZone(zone: ZoneId): Clock = VirtualClock(scheduler, start, zone)
    override fun instant(): Instant = start.plusMillis(scheduler.currentTime)
}
