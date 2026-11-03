package com.j4x.flowta.feature.integration.domain.usecase

import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Returns the id of the INCOME type called [name], creating it when it does not exist yet.
 * The repository's `add` returns no id, so the new row is found by diffing ids before and after.
 */
class CreateIncomeTypeUseCase @Inject constructor(
    private val types: TransactionTypeRepository
) {
    suspend operator fun invoke(name: String): Result<String> {
        val clean = name.trim()
        if (clean.isEmpty()) return Result.failure(IllegalArgumentException("Nombre vacío"))
        return try {
            val before = types.getByCategory(CATEGORY).first()
            before.firstOrNull { it.name.equals(clean, ignoreCase = true) }
                ?.let { return Result.success(it.id) }

            types.add(clean, CATEGORY).getOrElse { return Result.failure(it) }

            val knownIds = before.mapTo(HashSet()) { it.id }
            val created = types.getByCategory(CATEGORY).first()
                .firstOrNull { it.id !in knownIds && it.name == clean }
                ?: return Result.failure(IllegalStateException("Tipo no encontrado tras crearlo"))
            Result.success(created.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private companion object {
        const val CATEGORY = "INCOME"
    }
}
