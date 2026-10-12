package com.example.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

/** Value object. Arithmetic across currencies is a bug, so it fails fast. */
data class Money(val amount: BigDecimal, val currency: Currency) : Comparable<Money> {

    operator fun plus(other: Money) = sameCurrency(other) { Money(amount + other.amount, currency) }
    operator fun minus(other: Money) = sameCurrency(other) { Money(amount - other.amount, currency) }
    operator fun times(quantity: Int) = Money(amount * quantity.toBigDecimal(), currency)
    override fun compareTo(other: Money) = sameCurrency(other) { amount.compareTo(other.amount) }

    val isZero: Boolean get() = amount.signum() == 0

    fun coerceAtMost(max: Money): Money = if (this > max) max else this

    private inline fun <T> sameCurrency(other: Money, block: () -> T): T {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return block()
    }

    companion object {
        fun zero(currency: Currency) = Money(BigDecimal.ZERO.setScale(currency.defaultFractionDigits), currency)
        fun of(amount: String, currencyCode: String): Money {
            val currency = Currency.getInstance(currencyCode)
            return Money(BigDecimal(amount).setScale(currency.defaultFractionDigits, RoundingMode.HALF_EVEN), currency)
        }
    }
}
