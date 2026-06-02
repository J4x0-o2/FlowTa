package com.j4x.flowta.feature.accounts.domain.usecase

import com.j4x.flowta.feature.accounts.domain.repository.AccountRepository
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(private val repository: AccountRepository) {
    suspend operator fun invoke(id: String): Result<Unit> = repository.delete(id)
}
