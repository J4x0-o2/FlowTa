package com.j4x.flowta.feature.accounts.domain.repository

interface AccountResetRepository {
    /** Transactions plus transfers (as origin or destination) that a reset of [accountId] would delete. */
    suspend fun countMovements(accountId: String): Result<Long>

    /**
     * Atomically deletes every movement of [accountId] and sets its balance to [startingBalance].
     * Other accounts' stored balances are left untouched.
     */
    suspend fun reset(accountId: String, startingBalance: Double): Result<Unit>
}
