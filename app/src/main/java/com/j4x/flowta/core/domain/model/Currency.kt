package com.j4x.flowta.core.domain.model

data class Currency(val symbol: String, val code: String)

enum class CurrencyCode(val symbol: String, val displayCode: String) {
    COP("$", "COP"),
    USD("$", "USD"),
    EUR("€", "EUR");

    fun toCurrency() = Currency(symbol = symbol, code = displayCode)
}
