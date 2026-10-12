package com.example.core.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Currency

/**
 * An amount in one currency.
 *
 * The amount is normalised to the currency's fraction digits on construction, so equality does not
 * depend on how the amount was written ("5.0" equals "5.00"). Arithmetic across currencies is a
 * programming error and fails fast.
 */
class Money(amount: BigDecimal, val currency: Currency) : Comparable<Money> {

    val amount: BigDecimal = amount.setScale(currency.defaultFractionDigits, RoundingMode.HALF_EVEN)

    val minorUnits: Long get() = amount.movePointRight(currency.defaultFractionDigits).longValueExact()

    val isZero: Boolean get() = amount.signum() == 0

    operator fun plus(other: Money): Money = sameCurrency(other) { Money(amount + other.amount, currency) }

    operator fun minus(other: Money): Money = sameCurrency(other) { Money(amount - other.amount, currency) }

    operator fun times(quantity: Int): Money = Money(amount * quantity.toBigDecimal(), currency)

    override fun compareTo(other: Money): Int = sameCurrency(other) { amount.compareTo(other.amount) }

    fun coerceAtMost(max: Money): Money = if (this > max) max else this

    override fun equals(other: Any?): Boolean =
        other is Money && amount == other.amount && currency == other.currency

    override fun hashCode(): Int = 31 * amount.hashCode() + currency.hashCode()

    override fun toString(): String = "$amount ${currency.currencyCode}"

    private inline fun <T> sameCurrency(other: Money, block: () -> T): T {
        require(currency == other.currency) { "Currency mismatch: $currency vs ${other.currency}" }
        return block()
    }

    companion object {
        fun zero(currency: Currency): Money = Money(BigDecimal.ZERO, currency)

        fun of(amount: String, currencyCode: String): Money =
            Money(BigDecimal(amount), Currency.getInstance(currencyCode))

        fun ofMinor(minor: Long, currencyCode: String): Money {
            val currency = Currency.getInstance(currencyCode)
            return Money(BigDecimal.valueOf(minor, currency.defaultFractionDigits), currency)
        }
    }
}
