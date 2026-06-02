package com.j4x.flowta.feature.transactions.data.mapper

import com.j4x.flowta.database.SelectByAccount
import com.j4x.flowta.database.transaction.SelectAll
import com.j4x.flowta.feature.transactions.domain.model.Transaction

fun SelectAll.toDomain() = Transaction(
    id                = id,
    amount            = amount,
    flowType          = flow_type,
    note              = note,
    createdAt         = created_at,
    accountId         = account_id,
    accountName       = account_name,
    transactionTypeId = transaction_type_id,
    typeName          = type_name,
    typeCategory      = type_category,
    sourceTypeId      = source_type_id,
    sourceTypeName    = source_type_name
)

fun SelectByAccount.toDomain() = Transaction(
    id                = id,
    amount            = amount,
    flowType          = flow_type,
    note              = note,
    createdAt         = created_at,
    accountId         = account_id,
    accountName       = account_name,
    transactionTypeId = transaction_type_id,
    typeName          = type_name,
    typeCategory      = type_category,
    sourceTypeId      = source_type_id,
    sourceTypeName    = source_type_name
)
