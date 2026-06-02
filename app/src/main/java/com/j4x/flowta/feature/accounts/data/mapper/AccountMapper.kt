package com.j4x.flowta.feature.accounts.data.mapper

import com.j4x.flowta.database.Account
import com.j4x.flowta.feature.accounts.domain.model.Account as DomainAccount
import com.j4x.flowta.feature.accounts.domain.model.AccountType

fun Account.toDomain() = DomainAccount(
    id              = id,
    name            = name,
    type            = AccountType.valueOf(type),
    balance         = balance,
    metaAmount      = meta_amount,
    metaPeriodicity = meta_periodicity,
    createdAt       = created_at
)
