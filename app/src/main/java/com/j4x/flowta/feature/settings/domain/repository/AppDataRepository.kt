package com.j4x.flowta.feature.settings.domain.repository

interface AppDataRepository {
    /** Atomically deletes every row of every table (FK-safe order). All or nothing. */
    suspend fun wipeAll(): Result<Unit>
}
