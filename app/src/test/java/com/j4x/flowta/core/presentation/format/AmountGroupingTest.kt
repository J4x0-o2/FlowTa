package com.j4x.flowta.core.presentation.format

import androidx.compose.ui.text.AnnotatedString
import com.j4x.flowta.core.domain.model.CurrencyCode
import org.junit.Assert.assertEquals
import org.junit.Test

class AmountGroupingTest {

    private fun group(raw: String) = AmountGrouping(raw)

    // --- sanitizeAmountInput ---

    @Test
    fun sanitize_cop_keepsOnlyDigits() {
        assertEquals("373929", sanitizeAmountInput("373,929", CurrencyCode.COP))
        assertEquals("5000", sanitizeAmountInput("5.000", CurrencyCode.COP))
        assertEquals("12", sanitizeAmountInput("1a2-", CurrencyCode.COP))
    }

    @Test
    fun sanitize_cop_typedMarkMidNumber_keepsAllDigits() {
        // "1234" with a "." typed between 2 and 3.
        assertEquals("1234", sanitizeAmountInput("12.34", CurrencyCode.COP, previous = "1234"))
        assertEquals("1234", sanitizeAmountInput("12,34", CurrencyCode.COP, previous = "1234"))
    }

    @Test
    fun sanitize_cop_typedMarkThenDigitsAtEnd_keepsAllDigits() {
        assertEquals("1234", sanitizeAmountInput("1234.", CurrencyCode.COP, previous = "1234"))
        assertEquals("12345", sanitizeAmountInput("12345", CurrencyCode.COP, previous = "1234"))
    }

    @Test
    fun sanitize_cop_pastedFraction_isStillDropped() {
        assertEquals("1234", sanitizeAmountInput("1234.56", CurrencyCode.COP, previous = ""))
        assertEquals("1234", sanitizeAmountInput("1234.56", CurrencyCode.COP, previous = "9"))
        assertEquals("1234", sanitizeAmountInput("1234.56", CurrencyCode.COP))
    }

    @Test
    fun sanitize_usd_typing_isUnchanged() {
        assertEquals("12.", sanitizeAmountInput("12.", CurrencyCode.USD, previous = "12"))
        assertEquals("12.5", sanitizeAmountInput("12.5", CurrencyCode.USD, previous = "12."))
        assertEquals("12.34", sanitizeAmountInput("12.345", CurrencyCode.USD, previous = "12.34"))
        assertEquals("12.34", sanitizeAmountInput("12.34.", CurrencyCode.USD, previous = "12.34"))
    }

    @Test
    fun sanitize_usd_typedMarkMidNumber_neverDropsExistingDigits() {
        // "1234" + "." between 1 and 2 would become "1.23" and lose the "4": the mark is ignored.
        assertEquals("1234", sanitizeAmountInput("1.234", CurrencyCode.USD, previous = "1234"))
        // Fits in two fraction digits: nothing lost, the mark is kept.
        assertEquals("12.34", sanitizeAmountInput("12.34", CurrencyCode.USD, previous = "1234"))
    }

    @Test
    fun sanitize_usd_keepsOneDecimalMarkNormalizedToDot_andTwoDecimals() {
        assertEquals("1234.56", sanitizeAmountInput("1234.56", CurrencyCode.USD))
        assertEquals("12.5", sanitizeAmountInput("12,5", CurrencyCode.USD))
        assertEquals("12.", sanitizeAmountInput("12.", CurrencyCode.USD))
        assertEquals("12.34", sanitizeAmountInput("12.345", CurrencyCode.USD))
        assertEquals("12.34", sanitizeAmountInput("12.34.5", CurrencyCode.EUR))
        assertEquals(".5", sanitizeAmountInput(".5", CurrencyCode.USD))
    }

    @Test
    fun sanitize_cop_dropsPastedFractionInsteadOfInflating() {
        assertEquals("1234", sanitizeAmountInput("1234.56", CurrencyCode.COP))
        assertEquals("1234", sanitizeAmountInput("1234,5", CurrencyCode.COP))
        assertEquals("1234", sanitizeAmountInput("1.234,56", CurrencyCode.COP))
        assertEquals("5", sanitizeAmountInput("5.", CurrencyCode.COP))
    }

    @Test
    fun sanitize_cop_keepsThousandsGroupingAsInteger() {
        assertEquals("1234", sanitizeAmountInput("1.234", CurrencyCode.COP))
        assertEquals("1234567", sanitizeAmountInput("1,234,567", CurrencyCode.COP))
        assertEquals("1234567", sanitizeAmountInput("1.234.567", CurrencyCode.COP))
    }

    @Test
    fun sanitize_usd_pastedGroupingWithDecimals() {
        assertEquals("1234.56", sanitizeAmountInput("1,234.56", CurrencyCode.USD))
        assertEquals("1234.56", sanitizeAmountInput("1.234,56", CurrencyCode.EUR))
        assertEquals("1234567.8", sanitizeAmountInput("1,234,567.8", CurrencyCode.USD))
        assertEquals("1234567", sanitizeAmountInput("1,234,567", CurrencyCode.USD))
        assertEquals("1234567", sanitizeAmountInput("1.234.567", CurrencyCode.EUR))
    }

    @Test
    fun sanitize_usd_typingDigitByDigitStillWorks() {
        var field = ""
        "1234.56".forEach { field = sanitizeAmountInput(field + it, CurrencyCode.USD) }
        assertEquals("1234.56", field)
        assertEquals("12.34", sanitizeAmountInput("12.34.", CurrencyCode.USD))
    }

    @Test
    fun sanitize_output_isAlwaysAcceptedByParseAmount_whenComplete() {
        listOf("1234", "1234.5", "1234.56", "").forEach {
            assertEquals(true, parseAmount(sanitizeAmountInput(it, CurrencyCode.USD), CurrencyCode.USD) != null)
        }
    }

    // --- display text ---

    @Test
    fun text_groupsIntegerPartByThousands() {
        assertEquals("", group("").text)
        assertEquals("1", group("1").text)
        assertEquals("123", group("123").text)
        assertEquals("1,234", group("1234").text)
        assertEquals("373,929", group("373929").text)
        assertEquals("1,234,567", group("1234567").text)
    }

    @Test
    fun text_doesNotGroupTheDecimalPart() {
        assertEquals("1,234.56", group("1234.56").text)
        assertEquals("1,234.", group("1234.").text)
        assertEquals(".5", group(".5").text)
    }

    // --- offset mapping ---

    @Test
    fun toTransformed_keepsCursorRightAfterTheTypedDigit() {
        val g = group("1234") // "1,234"
        assertEquals(0, g.toTransformed(0))
        assertEquals(1, g.toTransformed(1)) // before the comma
        assertEquals(3, g.toTransformed(2)) // after "1,2"
        assertEquals(5, g.toTransformed(4)) // end
    }

    @Test
    fun toOriginal_countsOnlyRealCharacters() {
        val g = group("1234") // "1,234"
        assertEquals(0, g.toOriginal(0))
        assertEquals(1, g.toOriginal(1))
        assertEquals(1, g.toOriginal(2)) // after the comma, still 1 digit before
        assertEquals(2, g.toOriginal(3))
        assertEquals(4, g.toOriginal(5))
    }

    @Test
    fun offsets_withDecimals_shiftByTheIntegerCommas() {
        val g = group("1234.56") // "1,234.56"
        assertEquals(8, g.toTransformed(7))
        assertEquals(7, g.toOriginal(8))
        assertEquals(7, g.toTransformed(6)) // raw "1234.5|6"
    }

    @Test
    fun offsets_roundTrip_forEveryRawPosition() {
        listOf("", "7", "123", "1234", "123456", "1234567", "1234567.89", "99999.9").forEach { raw ->
            val g = group(raw)
            for (o in 0..raw.length) {
                assertEquals("raw=$raw o=$o", o, g.toOriginal(g.toTransformed(o)))
            }
        }
    }

    @Test
    fun offsets_neverExceedBounds() {
        val g = group("1234567")
        for (t in 0..g.text.length) {
            assert(g.toOriginal(t) in 0..7)
        }
    }

    // --- VisualTransformation wrapper ---

    @Test
    fun visualTransformation_wiresTextAndMapping() {
        val result = AmountVisualTransformation().filter(AnnotatedString("373929"))
        assertEquals("373,929", result.text.text)
        assertEquals(7, result.offsetMapping.originalToTransformed(6))
        assertEquals(6, result.offsetMapping.transformedToOriginal(7))
    }
}
