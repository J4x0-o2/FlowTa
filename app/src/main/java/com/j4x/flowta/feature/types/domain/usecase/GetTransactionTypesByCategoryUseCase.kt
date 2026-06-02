package com.j4x.flowta.feature.types.domain.usecase

import com.j4x.flowta.feature.types.domain.model.TransactionType
import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionTypesByCategoryUseCase @Inject constructor(
    private val repository: TransactionTypeRepository
) {
    operator fun invoke(category: String): Flow<List<TransactionType>> =
        repository.getByCategory(category)
}
