package com.j4x.flowta.core.presentation.format

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.j4x.flowta.core.domain.model.CurrencyCode

private const val DECIMAL = '.'
private const val GROUP = ','
private const val MAX_FRACTION_DIGITS = 2

/**
 * Cleans text typed or pasted into an amount field so the raw value never contains grouping.
 *
 * [previous] is the raw value before the edit. When given, a single-character edit is treated as
 * typing and never deletes digits (COP: a typed mark is simply dropped; USD/EUR: a mark that would
 * push digits out of the two-digit fraction is ignored). Without it, or for a multi-character
 * insertion, the text is handled as a paste.
 *
 * - COP: digits only. A pasted trailing fraction ("1234.56", "1.234,5": a mark followed by 1-2
 *   digits at the very end) is dropped, never merged into the integer; thousands grouping
 *   ("5.000", "1,234,567": groups of exactly 3 digits) becomes the integer.
 * - USD/EUR: digits plus at most one decimal mark normalized to ".", with at most two fraction
 *   digits. Pasted grouping is understood: with both marks the last one is the decimal
 *   ("1,234.56" and "1.234,56" -> "1234.56"); a repeated mark that is well-formed thousands
 *   grouping ("1,234,567") is dropped. Otherwise the first mark typed is the decimal and any
 *   later mark is ignored, which keeps digit-by-digit typing stable.
 *
 * The result is always accepted by [parseAmount] once complete (a trailing "." is the only
 * transient state and parses as the integer typed so far).
 */
fun sanitizeAmountInput(text: String, code: CurrencyCode, previous: String? = null): String {
    val typed = previous != null && !isPaste(text, previous)
    if (code == CurrencyCode.COP) return if (typed) text.digits() else sanitizeInteger(text)
    if (!typed) return sanitizePastedGrouping(text) ?: sanitizeTyped(text)
    val result = sanitizeTyped(text)
    // A single typed character must never delete digits that were already there.
    return if (result.digits().length < previous!!.digits().length) previous else result
}

/** A paste inserts more than one character at once; a single keystroke grows the text by <= 1. */
private fun isPaste(text: String, previous: String) = text.length - previous.length > 1

private fun String.digits() = filter { it.isDigit() }

private fun sanitizeInteger(text: String): String {
    val fraction = Regex("""^(.*)[.,](\d{1,2})$""").matchEntire(text)
    return (fraction?.groupValues?.get(1) ?: text).digits()
}

/** True for "1", "12.345", "1.234.567": a 1-3 digit head followed by groups of exactly 3 digits. */
private fun isThousandsGrouped(text: String, mark: Char): Boolean {
    val parts = text.split(mark)
    return parts.first().length in 1..3 && parts.drop(1).all { it.length == 3 } &&
        parts.all { part -> part.all { it.isDigit() } }
}

/** Handles pasted text whose marks are unambiguously grouping and/or decimal; null otherwise. */
private fun sanitizePastedGrouping(text: String): String? {
    val t = text.filter { it.isDigit() || it == '.' || it == ',' }
    val last = t.indexOfLast { it == '.' || it == ',' }
    if (last < 0) return null
    val mark = t[last]
    val other = if (mark == '.') ',' else '.'
    val tail = t.substring(last + 1)
    val head = t.substring(0, last)
    return when {
        // Both marks present: the last one is the decimal, the other one must group the head.
        other in head && mark !in head ->
            if (tail.length in 1..MAX_FRACTION_DIGITS && isThousandsGrouped(head, other)) {
                "${head.digits()}$DECIMAL$tail"
            } else null
        // Repeated single mark: thousands grouping only when the whole text is well-formed.
        mark in head && other !in head ->
            if (isThousandsGrouped(t, mark)) t.digits() else null
        else -> null
    }
}

private fun sanitizeTyped(text: String): String {
    val out = StringBuilder()
    var hasMark = false
    var fractionDigits = 0
    for (c in text) {
        when {
            c.isDigit() -> {
                if (hasMark) {
                    if (fractionDigits < MAX_FRACTION_DIGITS) {
                        out.append(c)
                        fractionDigits++
                    }
                } else {
                    out.append(c)
                }
            }
            (c == '.' || c == ',') && !hasMark -> {
                out.append(DECIMAL)
                hasMark = true
            }
        }
    }
    return out.toString()
}

/**
 * Display form of a raw amount ("1234.56" -> "1,234.56") plus the cursor mapping between the
 * raw and displayed strings. Only the integer part is grouped; the decimal part is verbatim.
 */
class AmountGrouping(private val raw: String) {
    private val integerLength = raw.indexOf(DECIMAL).let { if (it < 0) raw.length else it }

    // Number of separators inserted strictly before the raw digit at index i (0..integerLength).
    private fun separatorsBefore(i: Int): Int {
        var count = 0
        for (d in 1 until minOf(i, integerLength)) {
            if ((integerLength - d) % 3 == 0) count++
        }
        return count
    }

    val text: String = buildString {
        for (i in raw.indices) {
            if (i in 1 until integerLength && (integerLength - i) % 3 == 0) append(GROUP)
            append(raw[i])
        }
    }

    /** Raw cursor offset -> displayed offset; stays right after the digit that was just typed. */
    fun toTransformed(offset: Int): Int {
        val o = offset.coerceIn(0, raw.length)
        return o + separatorsBefore(o)
    }

    /** Displayed cursor offset -> raw offset: counts only the real (non-separator) characters. */
    fun toOriginal(offset: Int): Int {
        val t = offset.coerceIn(0, text.length)
        return text.substring(0, t).count { it != GROUP }
    }
}

/** Shows the thousands separator inside the field while the stored value stays raw. */
class AmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val grouping = AmountGrouping(text.text)
        return TransformedText(
            AnnotatedString(grouping.text),
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = grouping.toTransformed(offset)
                override fun transformedToOriginal(offset: Int) = grouping.toOriginal(offset)
            }
        )
    }
}
