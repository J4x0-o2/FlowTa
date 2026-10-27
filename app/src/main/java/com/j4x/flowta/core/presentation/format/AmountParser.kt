package com.j4x.flowta.core.presentation.format

import com.j4x.flowta.core.domain.model.CurrencyCode

/**
 * Parses a user-typed money amount by currency. Blank means 0; negatives, letters and anything
 * ambiguous are null. Every amount input in the app must go through this function.
 *
 * - COP (no decimals): "." and "," are only thousands separators and must be well placed
 *   ("5.000", "5,000" and "1.000.000" are 5000 / 1000000; "5,5" or "1.00.000" are invalid).
 * - USD/EUR: a single "." or "," followed by 1-2 digits is the decimal mark ("12.5", "12,34").
 *   With both marks the last one is the decimal ("1,234.56", "1.234,56"). Repeated marks are
 *   thousands grouping. A lone mark followed by exactly 3 digits ("5.000") is ambiguous and rejected.
 */
fun parseAmount(text: String, code: CurrencyCode): Double? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return 0.0
    if (!trimmed.all { it.isDigit() || it == '.' || it == ',' }) return null

    val dots = trimmed.count { it == '.' }
    val commas = trimmed.count { it == ',' }
    val value = when {
        dots == 0 && commas == 0 -> trimmed.toDoubleOrNull()
        code == CurrencyCode.COP -> {
            if (dots > 0 && commas > 0) return null
            val sep = if (dots > 0) '.' else ','
            if (isGrouped(trimmed, sep)) trimmed.filter { it.isDigit() }.toDoubleOrNull() else null
        }
        dots > 0 && commas > 0 -> {
            val decimal = if (trimmed.lastIndexOf('.') > trimmed.lastIndexOf(',')) '.' else ','
            val grouping = if (decimal == '.') ',' else '.'
            val (int, frac) = trimmed.split(decimal).let { it.dropLast(1).joinToString("") to it.last() }
            if (trimmed.count { it == decimal } == 1 && isGrouped(int, grouping) && frac.length in 1..2) {
                "${int.filter { it.isDigit() }}.$frac".toDoubleOrNull()
            } else null
        }
        else -> {
            val sep = if (dots > 0) '.' else ','
            if (dots + commas > 1) {
                if (isGrouped(trimmed, sep)) trimmed.filter { it.isDigit() }.toDoubleOrNull() else null
            } else {
                val (int, frac) = trimmed.split(sep)
                if (int.isNotEmpty() && frac.length in 1..2) "$int.$frac".toDoubleOrNull() else null
            }
        }
    }
    return value?.takeIf { it >= 0.0 && it.isFinite() }
}

/** True for "1", "12.345", "1.234.567": a 1-3 digit head followed by groups of exactly 3 digits. */
private fun isGrouped(text: String, separator: Char): Boolean {
    val parts = text.split(separator)
    return parts.first().length in 1..3 && parts.drop(1).all { it.length == 3 } &&
        parts.all { part -> part.all { it.isDigit() } }
}
