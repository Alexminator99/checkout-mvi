package com.example.core.ui

import com.example.core.domain.Money
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class LocaleMoneyFormatterTest {

    @Test
    fun `formats with the given locale and the money's own currency`() {
        val formatter = LocaleMoneyFormatter { Locale.US }

        assertEquals("€12.99", formatter.format(Money.of("12.99", "EUR")))
    }

    @Test
    fun `shows the currency's two fraction digits under a locale with none`() {
        val formatter = LocaleMoneyFormatter { Locale.JAPAN }

        assertEquals("€12.99", formatter.format(Money.of("12.99", "EUR")))
    }

    @Test
    fun `shows a zero-decimal currency without fraction digits under a two-digit locale`() {
        val formatter = LocaleMoneyFormatter { Locale.US }

        assertEquals("¥1,300", formatter.format(Money.of("1300", "JPY")))
    }

    @Test
    fun `applies the given locale's separators and symbol placement`() {
        val formatter = LocaleMoneyFormatter { Locale.GERMANY }

        assertEquals("12,99\u00A0€", formatter.format(Money.of("12.99", "EUR")))
    }
}
