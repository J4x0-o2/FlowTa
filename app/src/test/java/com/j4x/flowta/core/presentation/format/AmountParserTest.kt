package com.j4x.flowta.core.presentation.format

import com.j4x.flowta.core.domain.model.CurrencyCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AmountParserTest {

    private fun cop(text: String) = parseAmount(text, CurrencyCode.COP)
    private fun usd(text: String) = parseAmount(text, CurrencyCode.USD)

    @Test
    fun parseAmount_blankIsZero_forEveryCurrency() {
        assertEquals(0.0, cop("")!!, 0.0)
        assertEquals(0.0, cop("  ")!!, 0.0)
        assertEquals(0.0, usd("")!!, 0.0)
    }

    @Test
    fun parseAmount_cop_separatorsAreThousandsGroupingNeverDecimals() {
        assertEquals(5000.0, cop("5000")!!, 0.0)
        assertEquals(5000.0, cop("5.000")!!, 0.0)
        assertEquals(5000.0, cop("5,000")!!, 0.0)
        assertEquals(1_000_000.0, cop("1.000.000")!!, 0.0)
        assertEquals(1_000_000.0, cop("1,000,000")!!, 0.0)
        assertEquals(1500.0, cop("1.500")!!, 0.0)
    }

    @Test
    fun parseAmount_cop_rejectsAmbiguousOrMalformedInput() {
        assertNull(cop("5,5"))        // decimals do not exist in COP
        assertNull(cop("12.50"))
        assertNull(cop("1.000,50"))
        assertNull(cop("1.00.000"))   // bad grouping
        assertNull(cop("1000.000"))   // first group too long
        assertNull(cop("5.0000"))
        assertNull(cop("5."))
        assertNull(cop(".5"))
        assertNull(cop("abc"))
        assertNull(cop("-3"))
    }

    @Test
    fun parseAmount_usd_acceptsOneDecimalSeparatorWithOneOrTwoDigits() {
        assertEquals(12.5, usd("12.5")!!, 0.0)
        assertEquals(12.5, usd("12,5")!!, 0.0)
        assertEquals(12.34, usd("12.34")!!, 0.0)
        assertEquals(5000.0, usd("5000")!!, 0.0)
    }

    @Test
    fun parseAmount_usd_groupingIsAcceptedWhenUnambiguous() {
        assertEquals(1_000_000.0, usd("1.000.000")!!, 0.0)
        assertEquals(1_000_000.0, usd("1,000,000")!!, 0.0)
        assertEquals(1234.56, usd("1,234.56")!!, 0.0)
        assertEquals(1234.56, usd("1.234,56")!!, 0.0)
    }

    @Test
    fun parseAmount_usd_rejectsAmbiguousOrMalformedInput() {
        assertNull(usd("5.000"))      // 5 or 5000? do not guess
        assertNull(usd("5,000"))
        assertNull(usd("12.345"))
        assertNull(usd("1.234,567"))
        assertNull(usd("1,234.5.6"))
        assertNull(usd("1.000.00"))
        assertNull(usd("5."))
        assertNull(usd("abc"))
        assertNull(usd("-3"))
    }
}
