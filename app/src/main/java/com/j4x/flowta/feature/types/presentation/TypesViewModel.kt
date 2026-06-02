package com.j4x.flowta.feature.types.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.j4x.flowta.feature.types.domain.repository.TransactionTypeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TypesViewModel @Inject constructor(
    private val repository: TransactionTypeRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TypesState())
    val state = _state.asStateFlow()

    init {
        combine(
            repository.getByCategory("INCOME"),
            repository.getByCategory("EXPENSE"),
            repository.getByCategory("TRANSFER")
        ) { income, expense, transfer ->
            _state.update { it.copy(incomeTypes = income, expenseTypes = expense, transferTypes = transfer) }
        }.launchIn(viewModelScope)

        viewModelScope.launch { seedIfEmpty() }
    }

    fun onIntent(intent: TypesIntent) {
        when (intent) {
            is TypesIntent.AddType -> viewModelScope.launch {
                repository.add(intent.name, intent.category)
            }
            is TypesIntent.DeleteType -> viewModelScope.launch {
                repository.delete(intent.id)
            }
        }
    }

    private suspend fun seedIfEmpty() {
        if (repository.getAll().first().isNotEmpty()) return

        val defaults = mapOf(
            "INCOME" to listOf("Salario", "Freelance", "Ventas", "Inversiones", "Otro ingreso"),
            "EXPENSE" to listOf("Alimentación", "Hogar", "Transporte", "Salud", "Entretenimiento", "Educación", "Ropa", "Servicios", "Otro gasto"),
            "TRANSFER" to listOf("Entre cuentas", "A terceros"),
            "INCOME_SOURCE" to listOf("Efectivo", "Transferencia", "Pago", "Salario", "Freelance", "Inversión", "Otro")
        )
        defaults.forEach { (category, names) ->
            names.forEach { name -> repository.add(name, category) }
        }
    }
}
