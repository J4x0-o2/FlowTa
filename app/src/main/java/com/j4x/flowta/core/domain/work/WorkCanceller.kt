package com.j4x.flowta.core.domain.work

/** Cancels every piece of scheduled background work (e.g. goal reminders). */
interface WorkCanceller {
    fun cancelAll()
}
