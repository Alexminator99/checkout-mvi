package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.fake.FakeOrderRepository
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceOrderUseCaseTest {

    @Test
    fun `places the given request`() = runTest {
        val repository = FakeOrderRepository()
        val request = OrderRequest("checkout-1", HOME.id, STANDARD.id, VISA.id, promoCode = null)

        assertEquals(Outcome.Success(Order("order-1")), PlaceOrderUseCase(repository)(request))
        assertEquals(listOf(request), repository.placedRequests)
    }
}
