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

    private val repository = CartRepositoryImpl(InMemoryCartDataSource(), EUR, com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource(0))

    @Test
    fun `the demo cart holds a mug and a T-shirt`() = runTest {
        val cart = repository.observeCart().first()

        assertEquals(listOf("mug", "tee"), cart.lines.map { it.sku })
        assertEquals(eur("44.00"), cart.subtotal)
    }

    @Test
    fun `setting a quantity updates the cart`() = runTest {
        val result = repository.setQuantity("mug", 3)
        assertEquals(Outcome.Success(repository.observeCart().first()), result)

        assertEquals(3, repository.observeCart().first().lines.first { it.sku == "mug" }.quantity)
    }

    @Test
    fun `removing a line drops it from the cart`() = runTest {
        val result = repository.removeLine("mug")
        assertEquals(Outcome.Success(repository.observeCart().first()), result)

        assertEquals(listOf("tee"), repository.observeCart().first().lines.map { it.sku })
    }

    @Test
    fun `unknown lines are not found`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.NotFound), repository.setQuantity("nope", 1))
        assertEquals(Outcome.Failure(CheckoutError.NotFound), repository.removeLine("nope"))
    }
    @Test
    fun `refresh replaces stale product prices while preserving quantities`() = runTest {
        val source = InMemoryCartDataSource(listOf(
            com.example.feature.checkout.data.local.CartLineRecord("mug", "Coffee mug", 9900, "EUR", 3),
        ))
        val cart = CartRepositoryImpl(source, EUR, com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource(0))
        val result = cart.refresh() as Outcome.Success
        assertEquals(eur("36.00"), result.value.subtotal)
        assertEquals(3, result.value.lines.single().quantity)
        assertEquals(result.value, cart.observeCart().first())
    }

    @Test
    fun `restoring the demo refills the original cart`() = runTest {
        repository.removeLine("mug")
        repository.removeLine("tee")
        val result = repository.restoreDemoCart() as Outcome.Success
        assertEquals(eur("44.00"), result.value.subtotal)
        assertEquals(3, result.value.itemCount)
    }

}
