package com.j4x.flowta.feature.types.domain.usecase

import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/** Inserts the default transaction types when none exist (first launch, or after a full reset). */
class SeedDefaultTypesUseCase @Inject constructor(
    private val repository: TransactionTypeRepository
) {
    suspend operator fun invoke() {
        if (repository.getAll().first().isNotEmpty()) return

        DEFAULT_TYPES.forEach { (category, names) ->
            names.forEach { name -> repository.add(name, category).getOrThrow() }
        }
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
