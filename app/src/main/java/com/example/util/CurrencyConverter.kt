package com.example.util

import java.util.Locale

/**
 * Live multi-currency conversion utility for VoltLedger.
 * Provides realistic cross-rates for European and CIS EV drivers (BYN, PLN, EUR, USD, RUB, KZT)
 * and formats session/expense amounts in their original payment currency alongside main currency equivalent.
 */
object CurrencyConverter {
    val SUPPORTED_CURRENCIES = listOf("BYN", "PLN", "RUB", "EUR", "USD", "KZT")

    // Rates pegged to BYN base (1 BYN):
    // 1 BYN = 1.0 BYN
    // 1 PLN ≈ 0.82 BYN   (1 BYN ≈ 1.22 PLN)
    // 1 EUR ≈ 3.60 BYN   (1 BYN ≈ 0.28 EUR)
    // 1 USD ≈ 3.30 BYN   (1 BYN ≈ 0.30 USD)
    // 1 RUB ≈ 0.035 BYN  (100 RUB ≈ 3.50 BYN; 1 BYN ≈ 28.57 RUB)
    // 1 KZT ≈ 0.0068 BYN (1000 KZT ≈ 6.80 BYN; 1 BYN ≈ 147.0 KZT)
    val ratesToByn: Map<String, Double> = mapOf(
        "BYN" to 1.0,
        "PLN" to 0.82,
        "EUR" to 3.60,
        "USD" to 3.30,
        "RUB" to 0.035,
        "KZT" to 0.0068
    )

    /**
     * Converts [amount] from [fromCurrency] to [toCurrency].
     * If currencies match or rate is unknown, returns [amount].
     */
    fun convert(amount: Double, fromCurrency: String?, toCurrency: String?): Double {
        if (amount == 0.0) return 0.0
        val from = fromCurrency?.trim()?.uppercase()?.ifBlank { "BYN" } ?: "BYN"
        val to = toCurrency?.trim()?.uppercase()?.ifBlank { "BYN" } ?: "BYN"
        if (from == to) return amount

        val fromRate = ratesToByn[from] ?: 1.0
        val toRate = ratesToByn[to] ?: 1.0

        val inByn = amount * fromRate
        return inByn / toRate
    }

    /**
     * Formats an amount with its currency, and if different from targetCurrency,
     * appends the converted equivalent in parentheses: e.g. "50.00 PLN (~41.00 BYN)"
     */
    fun formatWithEquivalent(
        amount: Double,
        itemCurrency: String?,
        mainCurrency: String
    ): String {
        val curr = itemCurrency?.trim()?.uppercase()?.ifBlank { mainCurrency } ?: mainCurrency
        val baseStr = String.format(Locale.US, "%.2f %s", amount, curr)
        if (curr.equals(mainCurrency, ignoreCase = true)) {
            return baseStr
        }
        val converted = convert(amount, curr, mainCurrency)
        return String.format(Locale.US, "%.2f %s (~%.2f %s)", amount, curr, converted, mainCurrency)
    }

    /**
     * Returns a human-friendly cross-rate string comparing two currencies.
     */
    fun getRateHint(currency: String, baseCurrency: String = "BYN"): String {
        val curr = currency.uppercase()
        val base = baseCurrency.uppercase()
        if (curr == base) return "1.0 (Базовая валюта)"
        val rate = convert(1.0, curr, base)
        val revRate = if (rate > 0.0) 1.0 / rate else 1.0
        return String.format(Locale.US, "1 %s ≈ %.2f %s (1 %s ≈ %.2f %s)", curr, rate, base, base, revRate, curr)
    }
}
