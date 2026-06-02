package com.j4x.flowta.feature.profile.data.repository

import com.j4x.flowta.feature.accounts.domain.model.Account
import com.j4x.flowta.feature.accounts.domain.repository.AccountRepository
import com.j4x.flowta.feature.profile.domain.repository.ProfileRepository
import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer
import com.j4x.flowta.feature.transfers.domain.repository.TransferRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository
) : ProfileRepository {
    override fun getAccounts(): Flow<List<Account>> = accountRepository.getAll()
    override fun getTotalCapital(): Flow<Double> = accountRepository.getTotalBalance()
    override fun getTransfers(): Flow<List<InternalTransfer>> = transferRepository.getAll()
}
