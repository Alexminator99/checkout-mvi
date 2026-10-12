package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.local.InMemoryCartDataSource
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EUR
import com.example.feature.checkout.domain.fixture.eur
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class CartRepositoryImplTest {

    private val repository = CartRepositoryImpl(InMemoryCartDataSource(), EUR)

    @Test
    fun `the demo cart holds a mug and a T-shirt`() = runTest {
        val cart = repository.observeCart().first()

        assertEquals(listOf("mug", "tee"), cart.lines.map { it.sku })
        assertEquals(eur("44.00"), cart.subtotal)
    }

    @Test
    fun `setting a quantity updates the cart`() = runTest {
        assertEquals(Outcome.Success(Unit), repository.setQuantity("mug", 3))

        assertEquals(3, repository.observeCart().first().lines.first { it.sku == "mug" }.quantity)
    }

    @Test
    fun `removing a line drops it from the cart`() = runTest {
        assertEquals(Outcome.Success(Unit), repository.removeLine("mug"))

        assertEquals(listOf("tee"), repository.observeCart().first().lines.map { it.sku })
    }

    @Test
    fun `unknown lines are not found`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.NotFound), repository.setQuantity("nope", 1))
        assertEquals(Outcome.Failure(CheckoutError.NotFound), repository.removeLine("nope"))
    }
}
