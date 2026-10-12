package com.example.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.util.Currency

class MoneyTest {

    private val eur = Currency.getInstance("EUR")

    @Test
    fun `amounts are normalised to the currency scale`() {
        assertEquals(Money.of("5.00", "EUR"), Money(BigDecimal("5.0"), eur))
        assertEquals("5.00", Money(BigDecimal("5"), eur).amount.toPlainString())
    }

    @Test
    fun `equal amounts have equal hash codes`() {
        assertEquals(Money.of("5", "EUR").hashCode(), Money.of("5.00", "EUR").hashCode())
    }

    @Test
    fun `arithmetic keeps the currency`() {
        assertEquals(Money.of("17.50", "EUR"), Money.of("10.00", "EUR") + Money.of("7.50", "EUR"))
        assertEquals(Money.of("2.50", "EUR"), Money.of("10.00", "EUR") - Money.of("7.50", "EUR"))
        assertEquals(Money.of("36.00", "EUR"), Money.of("12.00", "EUR") * 3)
    }

    @Test
    fun `mixing currencies fails fast`() {
        assertThrows(IllegalArgumentException::class.java) {
            Money.of("1", "EUR") + Money.of("1", "USD")
        }
    }

    @Test
    fun `minor units convert both ways`() {
        val price = Money.ofMinor(1299, "EUR")

        assertEquals(Money.of("12.99", "EUR"), price)
        assertEquals(1299L, price.minorUnits)
    }

    @Test
    fun `coerceAtMost caps the amount`() {
        assertEquals(Money.of("10.00", "EUR"), Money.of("15.00", "EUR").coerceAtMost(Money.of("10.00", "EUR")))
        assertEquals(Money.of("5.00", "EUR"), Money.of("5.00", "EUR").coerceAtMost(Money.of("10.00", "EUR")))
    }

    @Test
    fun `zero is zero`() {
        assertTrue(Money.zero(eur).isZero)
        assertFalse(Money.of("0.01", "EUR").isZero)
    }
}
