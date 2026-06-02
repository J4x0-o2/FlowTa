package com.j4x.flowta.feature.metrics.domain.model

data class CategoryExpense(
    val typeId: String,
    val typeName: String,
    val category: String,
    val total: Double
)

data class DailyFlow(
    val flowType: String,
    val total: Double,
    val dayBucket: Long
)

data class DateRange(val from: Long, val to: Long)
