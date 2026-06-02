package com.j4x.flowta.feature.accounts.domain.usecase

import com.j4x.flowta.feature.accounts.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTotalCapitalUseCase @Inject constructor(private val repository: AccountRepository) {
    operator fun invoke(): Flow<Double> = repository.getTotalBalance()
}
