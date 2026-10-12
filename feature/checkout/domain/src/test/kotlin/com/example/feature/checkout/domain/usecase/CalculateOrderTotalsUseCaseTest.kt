package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.fixture.CUSTOMER
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.eur
import com.example.feature.checkout.domain.model.OrderTotals
import com.example.feature.checkout.domain.model.Promo
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculateOrderTotalsUseCaseTest {

    private val calculateOrderTotals = CalculateOrderTotalsUseCase()
    private val cart = cartOf(MUG) // 2 x 12.00

    @Test
    fun `a regular customer pays for shipping`() {
        assertEquals(eur("28.99"), calculateOrderTotals(cart, STANDARD, promo = null, customer = CUSTOMER).total)
    }

    @Test
    fun `members get standard shipping free but pay for express`() {
        assertEquals(eur("24.00"), calculateOrderTotals(cart, STANDARD, promo = null, customer = MEMBER).total)
        assertEquals(eur("36.99"), calculateOrderTotals(cart, EXPRESS, promo = null, customer = MEMBER).total)
    }

    @Test
    fun `the discount never exceeds the subtotal`() {
        val huge = Promo("ALL", eur("500.00"))

        assertEquals(eur("4.99"), calculateOrderTotals(cart, STANDARD, huge, CUSTOMER).total)
    }

    @Test
    fun `shipping costs nothing until an option is chosen`() {
        assertEquals(
            OrderTotals(subtotal = eur("24.00"), shipping = eur("0"), discount = eur("0"), total = eur("24.00")),
            calculateOrderTotals(cart, shipping = null, promo = null, customer = CUSTOMER),
        )
    }
}
