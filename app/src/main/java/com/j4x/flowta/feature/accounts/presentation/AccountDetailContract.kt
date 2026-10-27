package com.j4x.flowta.feature.accounts.presentation

import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.transactions.domain.model.Transaction

/** Warning dialog shown before resetting an account; [movementCount] = transactions + transfers. */
data class ResetAccountDialog(val movementCount: Long)

data class AccountDetailState(
    val account: Account? = null,
    val transactions: List<Transaction> = emptyList(),
    val selectedTransactionId: String? = null,
    val resetDialog: ResetAccountDialog? = null
)

sealed interface AccountDetailEffect {
    data class ShowMessage(val message: String) : AccountDetailEffect
}
