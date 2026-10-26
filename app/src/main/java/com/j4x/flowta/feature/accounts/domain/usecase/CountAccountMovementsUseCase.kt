package com.j4x.flowta.feature.accounts.domain.usecase

import com.j4x.flowta.feature.accounts.domain.repository.AccountResetRepository
import javax.inject.Inject

class CountAccountMovementsUseCase @Inject constructor(private val repository: AccountResetRepository) {
    suspend operator fun invoke(accountId: String): Result<Long> = repository.countMovements(accountId)
}
