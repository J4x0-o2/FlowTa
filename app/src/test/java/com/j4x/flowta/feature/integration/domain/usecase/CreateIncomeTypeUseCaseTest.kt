package com.j4x.flowta.feature.integration.domain.usecase

import com.j4x.flowta.feature.types.domain.model.TransactionType
import com.j4x.flowta.feature.types.domain.model.TypeCategory
import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateIncomeTypeUseCaseTest {

    private class FakeTypes(initial: List<TransactionType> = emptyList()) : TransactionTypeRepository {
        val all = MutableStateFlow(initial)
        var addFails = false
        var addCalls = 0
        override fun getAll(): Flow<List<TransactionType>> = all
        override fun getByCategory(category: String): Flow<List<TransactionType>> =
            all.map { list -> list.filter { it.category.name == category } }
        override suspend fun add(name: String, category: String): Result<Unit> {
            addCalls++
            if (addFails) return Result.failure(IllegalStateException("boom"))
            all.value = all.value + TransactionType("new-$addCalls", name, TypeCategory.valueOf(category), addCalls.toLong())
            return Result.success(Unit)
        }
        override suspend fun seedIfEmpty(types: List<Pair<String, String>>) = Result.success(Unit)
        override suspend fun delete(id: String) = Result.success(Unit)
    }

    @Test
    fun createsIncomeTypeAndReturnsItsId() = runBlocking {
        val repo = FakeTypes()
        val id = CreateIncomeTypeUseCase(repo)("  Carreras ").getOrThrow()

        assertEquals("new-1", id)
        assertEquals("Carreras", repo.all.value.single().name)
        assertEquals(TypeCategory.INCOME, repo.all.value.single().category)
    }

    @Test
    fun existingIncomeTypeWithSameName_isReusedWithoutDuplicating() = runBlocking {
        val repo = FakeTypes(listOf(TransactionType("t1", "carreras", TypeCategory.INCOME, 0L)))
        val id = CreateIncomeTypeUseCase(repo)("Carreras").getOrThrow()

        assertEquals("t1", id)
        assertEquals(0, repo.addCalls)
    }

    @Test
    fun sameNameInAnotherCategory_doesNotCount() = runBlocking {
        val repo = FakeTypes(listOf(TransactionType("e1", "Carreras", TypeCategory.EXPENSE, 0L)))
        val id = CreateIncomeTypeUseCase(repo)("Carreras").getOrThrow()

        assertEquals("new-1", id)
    }

    @Test
    fun blankName_fails() = runBlocking {
        assertTrue(CreateIncomeTypeUseCase(FakeTypes())("   ").isFailure)
    }

    @Test
    fun repositoryFailure_isPropagated() = runBlocking {
        val repo = FakeTypes().apply { addFails = true }
        assertTrue(CreateIncomeTypeUseCase(repo)("Carreras").isFailure)
    }
}
