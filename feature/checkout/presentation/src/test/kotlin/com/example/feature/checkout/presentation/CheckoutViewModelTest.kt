package com.example.feature.checkout.presentation

import app.cash.turbine.test
import com.example.core.domain.Outcome
import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fake.FakeAddressRepository
import com.example.feature.checkout.domain.fake.FakeOrderRepository
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import com.example.feature.checkout.presentation.ui.model.SummaryRowUi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The whole loop on the JVM: the real store, reducers, effect handler, use cases and projector over fake repositories. */
class CheckoutViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    /** Creates the ViewModel, keeps its UI state subscribed and lets the first load finish. */
    private fun TestScope.started(environment: CheckoutTestEnvironment = CheckoutTestEnvironment()): CheckoutViewModel {
        val viewModel = CheckoutViewModel(checkoutReducer(), environment.effectHandler(), checkoutProjector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        advanceUntilIdle()
        return viewModel
    }

    private val CheckoutViewModel.content: CheckoutUiState.Content
        get() = uiState.value as CheckoutUiState.Content

    @Test
    fun `starting shows the checkout with the defaults chosen`() = runTest {
        val content = started().content

        assertTrue(content.address.rows.first { it.id == HOME.id }.selected)
        assertTrue(content.payment.rows.first { it.id == VISA.id }.selected)
        assertTrue((content.shipping as ShippingSectionUi.Options).rows.first { it.id == PICKUP.id }.selected)
        // 2 x 12.00 mugs + 2.99 pickup, the cheapest option for a regular customer
        assertEquals(UiText.res(R.string.checkout_pay_amount, "€26.99"), content.placeOrder.label)
        assertTrue(content.placeOrder.enabled)
    }

    @Test
    fun `changing a quantity updates the cart and re-quotes shipping`() = runTest {
        val environment = CheckoutTestEnvironment()
        val viewModel = started(environment)

        viewModel.onIntent(CartAction.QuantityChanged(MUG.sku, 3))
        advanceUntilIdle()

        assertEquals(3, environment.cart.cart.value.lines.single().quantity)
        assertEquals(listOf(HOME.id, HOME.id), environment.shipping.quotedAddressIds)
    }

    @Test
    fun `applying a promo shows the discount`() = runTest {
        val environment = CheckoutTestEnvironment()
        val viewModel = started(environment)

        viewModel.onIntent(PromoAction.InputChanged(" save5 "))
        viewModel.onIntent(PromoAction.ApplyClicked)
        advanceUntilIdle()

        assertEquals(listOf("SAVE5"), environment.promos.redeemedCodes)
        assertEquals(
            SummaryRowUi(UiText.res(R.string.checkout_discount), UiText.res(R.string.checkout_discount_amount, "€5.00")),
            viewModel.content.summary.rows.last(),
        )
    }

    @Test
    fun `placing the order navigates to the confirmation once`() = runTest {
        val environment = CheckoutTestEnvironment()
        val viewModel = started(environment)

        viewModel.events.test {
            viewModel.onIntent(OrderAction.PlaceClicked)
            assertEquals(CheckoutEvent.NavigateToConfirmation("order-1"), awaitItem())
        }

        val request = environment.orders.placedRequests.single()
        assertEquals(OrderRequest(request.checkoutId, HOME.id, PICKUP.id, VISA.id, promoCode = null), request)
        assertTrue(request.checkoutId.isNotBlank())
    }

    @Test
    fun `a declined card shows a blocking error until dismissed`() = runTest {
        val environment = CheckoutTestEnvironment(orders = FakeOrderRepository(Outcome.Failure(CheckoutError.Payment.Declined)))
        val viewModel = started(environment)

        viewModel.onIntent(OrderAction.PlaceClicked)
        advanceUntilIdle()
        assertEquals(UiText.res(R.string.error_payment_declined), viewModel.content.blockingError)

        viewModel.onIntent(OrderAction.ErrorDismissed)
        advanceUntilIdle()
        assertNull(viewModel.content.blockingError)
        assertTrue(viewModel.content.placeOrder.enabled)
    }

    @Test
    fun `going offline shows the banner and blocks paying until reconnected`() = runTest {
        val environment = CheckoutTestEnvironment()
        val viewModel = started(environment)

        environment.network.online.value = false
        advanceUntilIdle()
        assertTrue(viewModel.content.isOffline)
        assertFalse(viewModel.content.placeOrder.enabled)

        environment.network.online.value = true
        advanceUntilIdle()
        assertFalse(viewModel.content.isOffline)
        assertTrue(viewModel.content.placeOrder.enabled)
    }

    @Test
    fun `a failed first load shows the error and retrying recovers`() = runTest {
        val environment = CheckoutTestEnvironment(
            addresses = FakeAddressRepository(addresses = Outcome.Failure(CheckoutError.Network)),
        )
        val viewModel = started(environment)
        assertEquals(CheckoutUiState.Error(UiText.res(R.string.error_network)), viewModel.uiState.value)

        environment.addresses.addresses = Outcome.Success(listOf(HOME, OFFICE))
        viewModel.onIntent(LifecycleAction.RetryLoadClicked)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is CheckoutUiState.Content)
    }
}
