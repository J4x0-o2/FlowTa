package com.j4x.flowta.feature.accounts.domain.usecase

import com.j4x.flowta.feature.accounts.domain.repository.AccountRepository
import javax.inject.Inject

class AddAccountUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(
        name: String,
        type: String,
        initialBalance: Double,
        metaAmount: Double?,
        metaPeriodicity: String?
    ): Result<Unit> = repository.add(name, type, initialBalance, metaAmount, metaPeriodicity)
}
