package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.fixture.CUSTOMER
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.eur
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingPriceTest {

    @Test
    fun `members ship non-express options for free`() {
        assertEquals(eur("0.00"), STANDARD.priceFor(MEMBER))
        assertEquals(eur("0.00"), PICKUP.priceFor(MEMBER))
    }

    @Test
    fun `members still pay for express`() {
        assertEquals(eur("12.99"), EXPRESS.priceFor(MEMBER))
    }

    @Test
    fun `regular customers pay the listed price`() {
        assertEquals(eur("4.99"), STANDARD.priceFor(CUSTOMER))
    }
}
