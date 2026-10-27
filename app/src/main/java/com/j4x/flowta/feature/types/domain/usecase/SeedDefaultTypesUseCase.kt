package com.j4x.flowta.feature.types.domain.usecase

import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

/** Inserts the default transaction types when none exist (first launch, or after a full reset). */
class SeedDefaultTypesUseCase @Inject constructor(
    private val repository: TransactionTypeRepository
) {
    /**
     * The emptiness check and the inserts run in ONE transaction (so a failure leaves nothing
     * half-seeded), and the mutex serializes callers inside this process. Never throws.
     */
    suspend operator fun invoke(): Result<Unit> = mutex.withLock {
        try {
            repository.seedIfEmpty(DEFAULT_TYPES.flatMap { (category, names) -> names.map { it to category } })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private val mutex = Mutex()

        val DEFAULT_TYPES: Map<String, List<String>> = mapOf(
            "INCOME" to listOf("Salario", "Freelance", "Ventas", "Inversiones", "Otro ingreso"),
            "EXPENSE" to listOf("Alimentación", "Hogar", "Transporte", "Salud", "Entretenimiento", "Educación", "Ropa", "Servicios", "Otro gasto"),
            "TRANSFER" to listOf("Entre cuentas", "A terceros"),
            "INCOME_SOURCE" to listOf("Efectivo", "Transferencia", "Pago", "Salario", "Freelance", "Inversión", "Otro")
        )
    }
}
