package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class OrderRepositoryImplTest {

    private val repository = OrderRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))

    private fun request(checkoutId: String = "checkout-1", paymentMethodId: String = DemoCheckoutData.VISA_ID) =
        OrderRequest(checkoutId, DemoCheckoutData.HOME_ID, "standard", paymentMethodId, promoCode = null)

    @Test
    fun `a valid card places the order`() = runTest {
        assertEquals(Outcome.Success(Order("ORD-1001")), repository.place(request()))
    }

    @Test
    fun `retrying the same request returns the same order`() = runTest {
        val first = repository.place(request())

        assertEquals(first, repository.place(request()))
    }

    @Test
    fun `the same order in a new checkout is a new order`() = runTest {
        val first = repository.place(request(checkoutId = "checkout-1"))

        assertNotEquals(first, repository.place(request(checkoutId = "checkout-2")))
    }

    @Test
    fun `the Mastercard is declined`() = runTest {
        assertEquals(
            Outcome.Failure(CheckoutError.Payment.Declined),
            repository.place(request(paymentMethodId = DemoCheckoutData.DECLINED_CARD_ID)),
        )
    }

    @Test
    fun `the Amex needs bank confirmation`() = runTest {
        assertEquals(
            Outcome.Failure(CheckoutError.Payment.RequiresAction),
            repository.place(request(paymentMethodId = DemoCheckoutData.CONFIRMATION_CARD_ID)),
        )
    }
}
