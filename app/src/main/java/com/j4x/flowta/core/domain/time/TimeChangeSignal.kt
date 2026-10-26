package com.j4x.flowta.core.domain.time

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

/** Emits whenever the device time zone, date or wall clock is changed by the system or the user. */
interface TimeChangeSignal {
    val changes: Flow<Unit>

    object None : TimeChangeSignal {
        override val changes: Flow<Unit> = emptyFlow()
    }
}
