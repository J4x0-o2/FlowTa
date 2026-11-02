package com.j4x.flowta.feature.integration.provider

import android.util.Log
import com.j4x.flowta.feature.integration.domain.model.ExternalIncome
import com.j4x.flowta.feature.integration.domain.model.ExternalPaymentMethod
import com.j4x.flowta.feature.integration.domain.model.RecordExternalIncomeResult

data class IntegrationReply(val status: String, val message: String? = null)

/**
 * Pure routing, parsing and status mapping for the provider. Extras are read through a plain
 * accessor so this is testable without an Android `Bundle`. [callingPackage] is the only trusted
 * identity of the caller: the optional `source_app` extra must match it, never replace it.
 */
class IntegrationDispatcher(
    private val recordIncome: (ExternalIncome) -> RecordExternalIncomeResult,
    private val isLinked: (sourceApp: String) -> Boolean
) {

    fun dispatch(method: String, callingPackage: String, extras: (String) -> Any?): IntegrationReply =
        try {
            when (method) {
                IntegrationContract.METHOD_RECORD_INCOME -> handleRecord(callingPackage, extras)
                IntegrationContract.METHOD_LINK_STATUS -> handleLinkStatus(callingPackage)
                else -> invalid("unknown method")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Integration call '$method' failed", e)
            IntegrationReply(IntegrationContract.STATUS_ERROR, "internal error")
        }

    private fun handleLinkStatus(callingPackage: String) = IntegrationReply(
        if (isLinked(callingPackage)) IntegrationContract.STATUS_LINKED else IntegrationContract.STATUS_NOT_LINKED
    )

    private fun handleRecord(callingPackage: String, extras: (String) -> Any?): IntegrationReply {
        val version = extras(IntegrationContract.EXTRA_CONTRACT_VERSION)
        if (version != IntegrationContract.CONTRACT_VERSION) return invalid("unsupported contract_version")

        val declaredSource = extras(IntegrationContract.EXTRA_SOURCE_APP)
        if (declaredSource != null && declaredSource != callingPackage) {
            return invalid("source_app does not match the calling package")
        }

        val externalId = extras(IntegrationContract.EXTRA_EXTERNAL_ID) as? String
            ?: return invalid("external_id must be a string")
        val method = (extras(IntegrationContract.EXTRA_PAYMENT_METHOD) as? String)
            ?.let { name -> ExternalPaymentMethod.entries.firstOrNull { it.name == name } }
            ?: return invalid("payment_method must be CASH or TRANSFER")
        val amountMinor = integral(extras(IntegrationContract.EXTRA_AMOUNT_MINOR))
            ?: return invalid("amount_minor must be a long")
        val occurredAt = integral(extras(IntegrationContract.EXTRA_OCCURRED_AT))
            ?: return invalid("occurred_at must be a long")
        val rawNote = extras(IntegrationContract.EXTRA_NOTE)
        if (rawNote != null && rawNote !is String) return invalid("note must be a string")

        val income = ExternalIncome(callingPackage, externalId, method, amountMinor, occurredAt, rawNote as String?)
        return when (val result = recordIncome(income)) {
            RecordExternalIncomeResult.Recorded -> IntegrationReply(IntegrationContract.STATUS_OK)
            RecordExternalIncomeResult.Duplicate -> IntegrationReply(IntegrationContract.STATUS_DUPLICATE)
            RecordExternalIncomeResult.NotLinked -> IntegrationReply(IntegrationContract.STATUS_NOT_LINKED)
            is RecordExternalIncomeResult.Invalid -> invalid(result.reason)
        }
    }

    private fun integral(value: Any?): Long? = when (value) {
        is Long -> value
        is Int -> value.toLong()
        else -> null
    }

    private fun invalid(message: String) = IntegrationReply(IntegrationContract.STATUS_INVALID, message)

    private companion object {
        const val TAG = "IntegrationProvider"
    }
}
