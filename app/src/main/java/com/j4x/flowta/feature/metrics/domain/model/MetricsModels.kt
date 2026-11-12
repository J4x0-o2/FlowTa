package com.j4x.flowta.feature.metrics.domain.model

data class CategoryExpense(
    val typeId: String,
    val typeName: String,
    val category: String,
    val total: Double
)

data class DateRange(val from: Long, val to: Long)

/** Raw income of one source in a range; both [sourceId] and [sourceName] are null for income without a source. */
data class IncomeSourceTotal(val sourceId: String?, val sourceName: String?, val total: Double)
