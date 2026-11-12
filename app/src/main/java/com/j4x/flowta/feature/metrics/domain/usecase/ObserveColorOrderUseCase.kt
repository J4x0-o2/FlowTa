package com.j4x.flowta.feature.metrics.domain.usecase

import com.j4x.flowta.feature.types.domain.model.TransactionType
import com.j4x.flowta.feature.types.domain.model.TypeCategory
import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Ids of the expense categories and income sources in a stable order (creation time, then id), which
 * decides their chart color slots. Independent of names, amounts and the shown period.
 */
data class ColorOrder(val expenseIds: List<String>, val sourceIds: List<String>)

class ObserveColorOrderUseCase @Inject constructor(
    private val repository: TransactionTypeRepository
) {
    operator fun invoke(): Flow<ColorOrder> = repository.getAll().map { types ->
        ColorOrder(
            expenseIds = types.idsOf(TypeCategory.EXPENSE, TypeCategory.RECURRING_EXPENSE),
            sourceIds = types.idsOf(TypeCategory.INCOME_SOURCE)
        )
    }

    private fun List<TransactionType>.idsOf(vararg categories: TypeCategory): List<String> =
        filter { it.category in categories }
            .sortedWith(compareBy<TransactionType> { it.createdAt }.thenBy { it.id })
            .map { it.id }
}
