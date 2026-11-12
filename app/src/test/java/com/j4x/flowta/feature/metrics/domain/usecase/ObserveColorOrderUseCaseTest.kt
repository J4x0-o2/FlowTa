package com.j4x.flowta.feature.metrics.domain.usecase

import com.j4x.flowta.feature.types.domain.model.TransactionType
import com.j4x.flowta.feature.types.domain.model.TypeCategory
import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveColorOrderUseCaseTest {

    private val all = MutableStateFlow<List<TransactionType>>(emptyList())

    private val repository = object : TransactionTypeRepository {
        override fun getAll(): Flow<List<TransactionType>> = all
        override fun getByCategory(category: String): Flow<List<TransactionType>> =
            all.map { list -> list.filter { it.category.name == category } }
        override suspend fun add(name: String, category: String) = Result.success(Unit)
        override suspend fun seedIfEmpty(types: List<Pair<String, String>>) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    private val useCase = ObserveColorOrderUseCase(repository)

    private fun type(id: String, category: TypeCategory, createdAt: Long, name: String = id) =
        TransactionType(id, name, category, createdAt)

    @Test
    fun expenseOrder_isCreatedAtThenId_andIncludesRecurringExpenses() = runTest {
        all.value = listOf(
            type("z", TypeCategory.EXPENSE, 10),
            type("b", TypeCategory.RECURRING_EXPENSE, 10),
            type("a", TypeCategory.EXPENSE, 30),
            type("m", TypeCategory.EXPENSE, 5)
        )
        assertEquals(listOf("m", "b", "z", "a"), useCase().first().expenseIds)
    }

    @Test
    fun sourceOrder_onlyIncomeSources_sameKey() = runTest {
        all.value = listOf(
            type("s2", TypeCategory.INCOME_SOURCE, 20),
            type("s1", TypeCategory.INCOME_SOURCE, 20),
            type("i", TypeCategory.INCOME, 1),
            type("e", TypeCategory.EXPENSE, 2),
            type("t", TypeCategory.TRANSFER, 3)
        )
        val order = useCase().first()
        assertEquals(listOf("s1", "s2"), order.sourceIds)
        assertEquals(listOf("e"), order.expenseIds)
    }

    @Test
    fun orderDoesNotDependOnNamesOrRepositoryOrdering() = runTest {
        val types = listOf(
            type("x", TypeCategory.EXPENSE, 1, name = "Zeta"),
            type("y", TypeCategory.EXPENSE, 2, name = "Alfa")
        )
        all.value = types
        val first = useCase().first().expenseIds
        all.value = types.reversed()
        assertEquals(first, useCase().first().expenseIds)
    }
}
