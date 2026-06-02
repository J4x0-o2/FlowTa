package com.j4x.flowta.feature.transfers.data.mapper

import com.j4x.flowta.database.internalTransfer.SelectAll
import com.j4x.flowta.feature.transfers.domain.model.InternalTransfer

fun SelectAll.toDomain() = InternalTransfer(
    id              = id,
    amount          = amount,
    note            = note,
    createdAt       = created_at,
    fromAccountId   = from_id,
    fromAccountName = from_name,
    toAccountId     = to_id,
    toAccountName   = to_name,
    transferTypeId  = transfer_type_id
)
