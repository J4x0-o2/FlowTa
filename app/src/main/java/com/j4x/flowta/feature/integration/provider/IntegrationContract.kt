package com.j4x.flowta.feature.integration.provider

/** Wire constants of the `ContentResolver.call` integration contract, version 1. */
object IntegrationContract {
    const val PERMISSION = "com.j4x.flowta.permission.RECORD_INCOME"
    const val CONTRACT_VERSION = 1

    const val METHOD_RECORD_INCOME = "recordIncome"
    const val METHOD_LINK_STATUS = "linkStatus"

    const val EXTRA_CONTRACT_VERSION = "contract_version"
    const val EXTRA_SOURCE_APP = "source_app"
    const val EXTRA_EXTERNAL_ID = "external_id"
    const val EXTRA_PAYMENT_METHOD = "payment_method"
    const val EXTRA_AMOUNT_MINOR = "amount_minor"
    const val EXTRA_OCCURRED_AT = "occurred_at"
    const val EXTRA_NOTE = "note"

    const val RESULT_STATUS = "status"
    const val RESULT_MESSAGE = "message"

    const val STATUS_OK = "OK"
    const val STATUS_DUPLICATE = "DUPLICATE"
    const val STATUS_NOT_LINKED = "NOT_LINKED"
    const val STATUS_INVALID = "INVALID"

    /** Unexpected failure on the FlowTa side; the caller may retry. */
    const val STATUS_ERROR = "ERROR"
    const val STATUS_LINKED = "LINKED"
}
