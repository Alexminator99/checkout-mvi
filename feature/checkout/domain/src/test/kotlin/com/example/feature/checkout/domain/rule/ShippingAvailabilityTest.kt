package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingAvailabilityTest {

    private val quotes = listOf(STANDARD, PICKUP, EXPRESS)

    @Test
    fun `express options are hidden while the flag is off`() {
        assertEquals(listOf(STANDARD, PICKUP), quotes.availableFor(isExpressEnabled = false))
    }

    @Test
    fun `every option is offered while the flag is on`() {
        assertEquals(quotes, quotes.availableFor(isExpressEnabled = true))
    }
}
