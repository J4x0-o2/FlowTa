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
 * - COP: digits only (no decimals).
 * - USD/EUR: digits plus at most one decimal mark (the first "." or "," typed, normalized to "."),
 *   with at most two fraction digits. Any later mark is dropped.
 *
 * The result is always accepted by [parseAmount] once complete (a trailing "." is the only
 * transient state and is still being typed).
 */
fun sanitizeAmountInput(text: String, code: CurrencyCode): String {
    if (code == CurrencyCode.COP) return text.filter { it.isDigit() }
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
