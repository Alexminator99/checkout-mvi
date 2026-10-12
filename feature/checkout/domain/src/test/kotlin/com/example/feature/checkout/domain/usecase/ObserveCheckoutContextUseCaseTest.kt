package com.example.feature.checkout.domain.usecase

import app.cash.turbine.test
import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fake.FakeAddressRepository
import com.example.feature.checkout.domain.fake.FakeCartRepository
import com.example.feature.checkout.domain.fake.FakeCustomerRepository
import com.example.feature.checkout.domain.fake.FakeFeatureFlagRepository
import com.example.feature.checkout.domain.fake.FakeNetworkMonitor
import com.example.feature.checkout.domain.fake.FakePaymentMethodRepository
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.TEE
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveCheckoutContextUseCaseTest {

    private val cartRepository = FakeCartRepository()
    private val addressRepository = FakeAddressRepository()
    private val paymentMethodRepository = FakePaymentMethodRepository()
    private val customerRepository = FakeCustomerRepository()
    private val networkMonitor = FakeNetworkMonitor()
    private val featureFlagRepository = FakeFeatureFlagRepository()

    private val observeCheckoutContext = ObserveCheckoutContextUseCase(
        cartRepository,
        addressRepository,
        paymentMethodRepository,
        customerRepository,
        networkMonitor,
        featureFlagRepository,
    )

    @Test
    fun `combines every source into one context`() = runTest {
        assertEquals(Outcome.Success(checkoutContext()), observeCheckoutContext().first())
    }

    @Test
    fun `a failed address fetch fails the context`() = runTest {
        addressRepository.addresses = Outcome.Failure(CheckoutError.Network)

        assertEquals(Outcome.Failure(CheckoutError.Network), observeCheckoutContext().first())
    }

    @Test
    fun `a failed payment method fetch fails the context`() = runTest {
        paymentMethodRepository.paymentMethods = Outcome.Failure(CheckoutError.Unauthorized)

        assertEquals(Outcome.Failure(CheckoutError.Unauthorized), observeCheckoutContext().first())
    }

    @Test
    fun `a cart change emits an updated context`() = runTest {
        observeCheckoutContext().test {
            assertEquals(Outcome.Success(checkoutContext()), awaitItem())

            cartRepository.cart.value = cartOf(MUG, TEE)

            assertEquals(Outcome.Success(checkoutContext(cart = cartOf(MUG, TEE))), awaitItem())
        }
    }

    @Test
    fun `losing connectivity emits an offline context`() = runTest {
        observeCheckoutContext().test {
            awaitItem()

            networkMonitor.online.value = false

            assertEquals(Outcome.Success(checkoutContext(isOnline = false)), awaitItem())
        }
    }
}
