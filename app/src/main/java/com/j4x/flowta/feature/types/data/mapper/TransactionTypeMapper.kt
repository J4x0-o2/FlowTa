package com.j4x.flowta.feature.types.data.mapper

import com.j4x.flowta.database.Transaction_type
import com.j4x.flowta.feature.types.domain.model.TransactionType
import com.j4x.flowta.feature.types.domain.model.TypeCategory

fun Transaction_type.toDomain() = TransactionType(
    id        = id,
    name      = name,
    category  = TypeCategory.valueOf(category),
    createdAt = created_at
)
