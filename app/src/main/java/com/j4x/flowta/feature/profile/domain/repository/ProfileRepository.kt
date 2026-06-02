package com.j4x.flowta.feature.profile.domain.repository

import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAccounts(): Flow<List<Account>>
    fun getTotalCapital(): Flow<Double>
    fun getTransfers(): Flow<List<InternalTransfer>>
}
