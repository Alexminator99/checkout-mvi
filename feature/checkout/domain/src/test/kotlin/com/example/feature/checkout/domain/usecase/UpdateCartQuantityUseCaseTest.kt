package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fake.FakeCartRepository
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.TEE
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.rule.MAX_QUANTITY_PER_LINE
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UpdateCartQuantityUseCaseTest {

    private val cartRepository = FakeCartRepository(cartOf(MUG, TEE))
    private val updateCartQuantity = UpdateCartQuantityUseCase(cartRepository)

    private fun quantityOf(sku: String) = cartRepository.cart.value.lines.first { it.sku == sku }.quantity

    @Test
    fun `sets the requested quantity`() = runTest {
        assertEquals(Outcome.Success(Unit), updateCartQuantity(MUG.sku, 3))
        assertEquals(3, quantityOf(MUG.sku))
    }

    @Test
    fun `zero removes the line`() = runTest {
        updateCartQuantity(MUG.sku, 0)

        assertEquals(listOf(TEE), cartRepository.cart.value.lines)
    }

    @Test
    fun `quantities above the maximum are capped`() = runTest {
        updateCartQuantity(MUG.sku, 99)

        assertEquals(MAX_QUANTITY_PER_LINE, quantityOf(MUG.sku))
    }

    @Test
    fun `a failed write is reported`() = runTest {
        cartRepository.failure = CheckoutError.Network

        assertEquals(Outcome.Failure(CheckoutError.Network), updateCartQuantity(MUG.sku, 3))
    }
}
