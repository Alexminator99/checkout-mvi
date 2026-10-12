package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.local.CartLineRecord
import com.example.feature.checkout.data.remote.dto.QuoteLineDto
import com.example.feature.checkout.domain.fixture.EUR
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.eur
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.CartLine
import org.junit.Assert.assertEquals
import org.junit.Test

class CartMapperTest {

    @Test
    fun `records become cart lines priced from minor units`() {
        val cart = listOf(CartLineRecord("mug", "Coffee mug", 1200, "EUR", 2)).toCart(EUR)

        assertEquals(cartOf(CartLine("mug", "Coffee mug", eur("12.00"), 2)), cart)
    }

    @Test
    fun `an empty cart uses the store currency`() {
        assertEquals(Cart(emptyList(), EUR), emptyList<CartLineRecord>().toCart(EUR))
    }

    @Test
    fun `cart lines become quote lines`() {
        assertEquals(listOf(QuoteLineDto("mug", 2, 1200, "EUR")), cartOf(MUG).toQuoteLines())
    }
}
