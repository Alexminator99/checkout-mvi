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
}
