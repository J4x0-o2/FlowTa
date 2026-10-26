package com.j4x.flowta.feature.types.domain.usecase

import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Inserts the default transaction types when none exist (first launch, or after a full reset). */
class SeedDefaultTypesUseCase @Inject constructor(
    private val repository: TransactionTypeRepository
) {
    /** All defaults go in ONE transaction, so a failure leaves nothing half-seeded. Never throws. */
    suspend operator fun invoke(): Result<Unit> {
        val existing = try {
            repository.getAll().first()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(e)
        }
        if (existing.isNotEmpty()) return Result.success(Unit)

        return repository.addAll(DEFAULT_TYPES.flatMap { (category, names) -> names.map { it to category } })
    }

    companion object {
        val DEFAULT_TYPES: Map<String, List<String>> = mapOf(
            "INCOME" to listOf("Salario", "Freelance", "Ventas", "Inversiones", "Otro ingreso"),
            "EXPENSE" to listOf("Alimentación", "Hogar", "Transporte", "Salud", "Entretenimiento", "Educación", "Ropa", "Servicios", "Otro gasto"),
            "TRANSFER" to listOf("Entre cuentas", "A terceros"),
            "INCOME_SOURCE" to listOf("Efectivo", "Transferencia", "Pago", "Salario", "Freelance", "Inversión", "Otro")
        )
    }
}
