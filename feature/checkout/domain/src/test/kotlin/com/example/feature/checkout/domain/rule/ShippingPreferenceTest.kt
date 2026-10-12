package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.fixture.CUSTOMER
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.model.ShippingOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShippingPreferenceTest {

    private val options = listOf(STANDARD, PICKUP, EXPRESS)

    @Test
    fun `keeps the current choice while it is still offered`() {
        assertEquals(EXPRESS, options.preferredFor(EXPRESS.id, CUSTOMER))
    }

    @Test
    fun `picks the cheapest option for a regular customer`() {
        assertEquals(PICKUP, options.preferredFor(currentId = null, customer = CUSTOMER))
    }

    @Test
    fun `prefers the faster option when the prices tie`() {
        // Members pay nothing for Standard or Pickup, so the faster Standard wins.
        assertEquals(STANDARD, options.preferredFor(currentId = null, customer = MEMBER))
    }

    @Test
    fun `replaces a choice that is no longer offered`() {
        assertEquals(PICKUP, options.preferredFor(currentId = "gone", customer = CUSTOMER))
    }

    @Test
    fun `nothing to choose from yields no choice`() {
        assertNull(emptyList<ShippingOption>().preferredFor(currentId = null, customer = CUSTOMER))
    }
}
