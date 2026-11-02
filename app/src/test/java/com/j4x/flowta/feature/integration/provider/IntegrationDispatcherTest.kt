package com.j4x.flowta.feature.integration.provider

import com.j4x.flowta.feature.integration.domain.model.ExternalIncome
import com.j4x.flowta.feature.integration.domain.model.ExternalPaymentMethod
import com.j4x.flowta.feature.integration.domain.model.RecordExternalIncomeResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IntegrationDispatcherTest {

    private val caller = "com.micuadre"
    private val recorded = mutableListOf<ExternalIncome>()
    private var recordResult: () -> RecordExternalIncomeResult = { RecordExternalIncomeResult.Recorded }
    private var linked: (String) -> Boolean = { true }

    private val dispatcher = IntegrationDispatcher(
        recordIncome = { recorded += it; recordResult() },
        isLinked = { linked(it) }
    )

    private fun validExtras(vararg overrides: Pair<String, Any?>): (String) -> Any? {
        val map = mutableMapOf<String, Any?>(
            IntegrationContract.EXTRA_CONTRACT_VERSION to 1,
            IntegrationContract.EXTRA_EXTERNAL_ID to "trip-1",
            IntegrationContract.EXTRA_PAYMENT_METHOD to "CASH",
            IntegrationContract.EXTRA_AMOUNT_MINOR to 1_550_000L,
            IntegrationContract.EXTRA_OCCURRED_AT to 1_700_000_000_000L,
            IntegrationContract.EXTRA_NOTE to "MiCuadre - Uber"
        )
        overrides.forEach { (k, v) -> if (v == null) map.remove(k) else map[k] = v }
        return { map[it] }
    }

    private fun record(extras: (String) -> Any?) =
        dispatcher.dispatch(IntegrationContract.METHOD_RECORD_INCOME, caller, extras)

    // --- parsing ---

    @Test
    fun validRequest_usesCallingPackageAsSource() {
        val reply = record(validExtras())

        assertEquals(IntegrationContract.STATUS_OK, reply.status)
        assertEquals(
            ExternalIncome(caller, "trip-1", ExternalPaymentMethod.CASH, 1_550_000L, 1_700_000_000_000L, "MiCuadre - Uber"),
            recorded.single()
        )
    }

    @Test
    fun transferMethod_andMissingNote_areAccepted() {
        record(validExtras(
            IntegrationContract.EXTRA_PAYMENT_METHOD to "TRANSFER",
            IntegrationContract.EXTRA_NOTE to null
        ))

        val income = recorded.single()
        assertEquals(ExternalPaymentMethod.TRANSFER, income.paymentMethod)
        assertNull(income.note)
    }

    @Test
    fun matchingSourceAppExtra_isAccepted() {
        val reply = record(validExtras(IntegrationContract.EXTRA_SOURCE_APP to caller))

        assertEquals(IntegrationContract.STATUS_OK, reply.status)
    }

    @Test
    fun mismatchingSourceAppExtra_isInvalid_andNeverReachesUseCase() {
        val reply = record(validExtras(IntegrationContract.EXTRA_SOURCE_APP to "com.evil"))

        assertInvalid(reply)
        assertTrue(recorded.isEmpty())
    }

    @Test
    fun wrongContractVersion_isInvalid() {
        assertInvalid(record(validExtras(IntegrationContract.EXTRA_CONTRACT_VERSION to 2)))
        assertInvalid(record(validExtras(IntegrationContract.EXTRA_CONTRACT_VERSION to null)))
        assertInvalid(record(validExtras(IntegrationContract.EXTRA_CONTRACT_VERSION to "1")))
        assertTrue(recorded.isEmpty())
    }

    @Test
    fun missingOrWrongTypedFields_areInvalid() {
        val bad = listOf(
            IntegrationContract.EXTRA_EXTERNAL_ID to null,
            IntegrationContract.EXTRA_EXTERNAL_ID to 42,
            IntegrationContract.EXTRA_PAYMENT_METHOD to null,
            IntegrationContract.EXTRA_PAYMENT_METHOD to "CARD",
            IntegrationContract.EXTRA_PAYMENT_METHOD to "cash",
            IntegrationContract.EXTRA_AMOUNT_MINOR to null,
            IntegrationContract.EXTRA_AMOUNT_MINOR to "100",
            IntegrationContract.EXTRA_AMOUNT_MINOR to 1.5,
            IntegrationContract.EXTRA_OCCURRED_AT to null,
            IntegrationContract.EXTRA_OCCURRED_AT to "now",
            IntegrationContract.EXTRA_NOTE to 7,
            IntegrationContract.EXTRA_SOURCE_APP to 7
        )
        bad.forEach { (key, value) ->
            assertInvalid(record(validExtras(key to value)))
        }
        assertTrue(recorded.isEmpty())
    }

    @Test
    fun integerAmounts_areWidenedToLong() {
        record(validExtras(IntegrationContract.EXTRA_AMOUNT_MINOR to 500))

        assertEquals(500L, recorded.single().amountMinor)
    }

    // --- status mapping ---

    @Test
    fun useCaseResults_mapToStatuses() {
        val cases = mapOf(
            RecordExternalIncomeResult.Recorded to IntegrationContract.STATUS_OK,
            RecordExternalIncomeResult.Duplicate to IntegrationContract.STATUS_DUPLICATE,
            RecordExternalIncomeResult.NotLinked to IntegrationContract.STATUS_NOT_LINKED,
            RecordExternalIncomeResult.Invalid("amount_minor must be positive") to IntegrationContract.STATUS_INVALID
        )
        cases.forEach { (result, status) ->
            recordResult = { result }
            assertEquals(status, record(validExtras()).status)
        }
    }

    @Test
    fun invalidResult_carriesReasonAsMessage() {
        recordResult = { RecordExternalIncomeResult.Invalid("amount_minor must be positive") }

        val reply = record(validExtras())

        assertEquals("amount_minor must be positive", reply.message)
    }

    @Test
    fun unexpectedException_mapsToError_withMessage() {
        recordResult = { throw IllegalStateException("disk full") }

        val reply = record(validExtras())

        assertEquals(IntegrationContract.STATUS_ERROR, reply.status)
        assertNotNull(reply.message)
    }

    // --- linkStatus and dispatch ---

    @Test
    fun linkStatus_reflectsLinkOfCallingPackage() {
        var asked: String? = null
        linked = { asked = it; true }
        assertEquals(
            IntegrationContract.STATUS_LINKED,
            dispatcher.dispatch(IntegrationContract.METHOD_LINK_STATUS, caller, { null }).status
        )
        assertEquals(caller, asked)

        linked = { false }
        assertEquals(
            IntegrationContract.STATUS_NOT_LINKED,
            dispatcher.dispatch(IntegrationContract.METHOD_LINK_STATUS, caller, { null }).status
        )
    }

    @Test
    fun linkStatus_exception_mapsToError() {
        linked = { throw RuntimeException("db closed") }

        val reply = dispatcher.dispatch(IntegrationContract.METHOD_LINK_STATUS, caller, { null })

        assertEquals(IntegrationContract.STATUS_ERROR, reply.status)
    }

    @Test
    fun unknownMethod_isInvalid() {
        assertInvalid(dispatcher.dispatch("dropEverything", caller, validExtras()))
    }

    private fun assertInvalid(reply: IntegrationReply) {
        assertEquals(IntegrationContract.STATUS_INVALID, reply.status)
        assertTrue(!reply.message.isNullOrBlank())
    }
}
