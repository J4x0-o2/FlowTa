package com.j4x.flowta.feature.integration.domain.model

enum class ExternalPaymentMethod { CASH, TRANSFER }

/** An income reported by another app. [amountMinor] is whole currency units x 100. */
data class ExternalIncome(
    val sourceApp: String,
    val externalId: String,
    val paymentMethod: ExternalPaymentMethod,
    val amountMinor: Long,
    val occurredAt: Long,
    val note: String?
)

sealed interface RecordExternalIncomeResult {
    data object Recorded : RecordExternalIncomeResult
    data object Duplicate : RecordExternalIncomeResult
    data object NotLinked : RecordExternalIncomeResult
    data class Invalid(val reason: String) : RecordExternalIncomeResult
}
