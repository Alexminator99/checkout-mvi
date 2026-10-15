package com.example.feature.checkout.presentation

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fake.FakeOrderRepository
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.fixture.eur
import com.example.feature.checkout.presentation.contract.*
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CheckoutConsistencyTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private val reducer = checkoutReducer()

    private fun TestScope.started(environment: CheckoutTestEnvironment): CheckoutViewModel {
        val vm = CheckoutViewModel(reducer, environment.effectHandler(), checkoutProjector())
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        advanceUntilIdle()
        return vm
    }
    private val CheckoutViewModel.content get() = uiState.value as CheckoutUiState.Content

    @Test fun `a pending cart write blocks payment and overlapping writes`() {
        val pending = reducer.reduce(readyState(), CartAction.QuantityChanged(MUG.sku, 3)).state
        assertNull(pending.orderRequestOrNull())
        assertTrue(reducer.reduce(pending, CartAction.QuantityChanged(MUG.sku, 4)).effects.isEmpty())
        assertTrue(reducer.reduce(pending, OrderAction.PlaceClicked).effects.isEmpty())
    }

    @Test fun `a delayed cart write cannot be overtaken by payment`() = runTest {
        val environment = CheckoutTestEnvironment()
        val vm = started(environment)
        val gate = CompletableDeferred<Unit>()
        environment.cart.writeGate = gate
        vm.onIntent(CartAction.QuantityChanged(MUG.sku, 3))
        advanceUntilIdle()
        assertFalse(vm.content.placeOrder.enabled)
        assertFalse(vm.content.cart.lines.single().canIncrement)
        vm.onIntent(OrderAction.PlaceClicked)
        advanceUntilIdle()
        assertTrue(environment.orders.placedRequests.isEmpty())
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(3, vm.content.cart.lines.single().quantity)
        assertTrue(vm.content.placeOrder.enabled)
    }

    @Test fun `failed cart writes release the pending guard`() = runTest {
        val environment = CheckoutTestEnvironment()
        val vm = started(environment)
        environment.cart.failure = CheckoutError.Network
        vm.onIntent(CartAction.QuantityChanged(MUG.sku, 3))
        advanceUntilIdle()
        assertEquals(2, vm.content.cart.lines.single().quantity)
        assertTrue(vm.content.placeOrder.enabled)
    }

    @Test fun `payment keeps its visible cart and defers the latest context`() {
        val initial = readyState().copy(submission = Submission.Submitting)
        val changed = checkoutContext(cart = cartOf(MUG.copy(quantity = 5)), isOnline = false)
        val pending = reducer.reduce(initial, ContextAction.Updated(changed)).state
        assertEquals(initial.contextOrNull!!.cart, pending.contextOrNull!!.cart)
        assertFalse(pending.contextOrNull!!.isOnline)
        assertEquals(changed, pending.deferredContext)
        val failed = reducer.reduce(pending, OrderAction.Failed(CheckoutError.Payment.Declined)).state
        assertEquals(changed.cart, failed.contextOrNull!!.cart)
        assertNull(failed.deferredContext)
    }

    @Test fun `editing an address under the same id invalidates delivery and quotes`() {
        val changed = checkoutContext(addresses = listOf(HOME.copy(line1 = "PO Box 42")))
        val transition = reducer.reduce(readyState(), ContextAction.Updated(changed))
        assertEquals(Loadable.Loading, transition.state.address.validation)
        assertEquals(Loadable.Loading, transition.state.shipping.quotes)
        assertNull(transition.state.orderRequestOrNull())
        assertEquals(1, transition.effects.filterIsInstance<CheckoutEffect.ValidateAddress>().size)
        assertEquals(1, transition.effects.filterIsInstance<CheckoutEffect.LoadShippingQuotes>().size)
    }

    @Test fun `an address edit and cart update request quotes only once`() {
        val changed = checkoutContext(
            addresses = listOf(HOME.copy(line1 = "Different street")),
            cart = cartOf(MUG.copy(quantity = 4)),
        )
        val transition = reducer.reduce(readyState(), ContextAction.Updated(changed))
        assertEquals(1, transition.effects.filterIsInstance<CheckoutEffect.LoadShippingQuotes>().size)
    }

    @Test fun `price conflicts refresh product prices before retry is possible`() = runTest {
        val environment = CheckoutTestEnvironment(orders = FakeOrderRepository(Outcome.Failure(CheckoutError.PriceChanged)))
        environment.cart.refreshedCart = cartOf(MUG.copy(unitPrice = eur("15.00")))
        val vm = started(environment)
        vm.onIntent(OrderAction.PlaceClicked)
        advanceUntilIdle()
        assertEquals(1, environment.cart.refreshCount)
        assertEquals("€32.99", vm.content.summary.total)
        assertNotNull(vm.content.blockingError)
        assertFalse(vm.content.placeOrder.enabled)
        vm.onIntent(OrderAction.ErrorDismissed)
        advanceUntilIdle()
        assertTrue(vm.content.placeOrder.enabled)
    }

    @Test fun `failed price refresh stays blocked and the dialog can retry it`() = runTest {
        val environment = CheckoutTestEnvironment(orders = FakeOrderRepository(Outcome.Failure(CheckoutError.PriceChanged)))
        environment.cart.refreshFailure = CheckoutError.Network
        val vm = started(environment)
        vm.onIntent(OrderAction.PlaceClicked)
        advanceUntilIdle()
        assertTrue(vm.content.retryPriceRefresh)
        assertFalse(vm.content.placeOrder.enabled)
        environment.cart.refreshFailure = null
        vm.onIntent(OrderAction.ErrorDismissed)
        advanceUntilIdle()
        assertEquals(2, environment.cart.refreshCount)
        assertFalse(vm.content.retryPriceRefresh)
        vm.onIntent(OrderAction.ErrorDismissed)
        advanceUntilIdle()
        assertTrue(vm.content.placeOrder.enabled)
    }

    @Test fun `reset restores an emptied demo cart and clears the promo`() = runTest {
        val environment = CheckoutTestEnvironment()
        val vm = started(environment)
        vm.onIntent(PromoAction.InputChanged("SAVE5"))
        vm.onIntent(PromoAction.ApplyClicked)
        advanceUntilIdle()
        vm.onIntent(CartAction.QuantityChanged(MUG.sku, 0))
        advanceUntilIdle()
        assertTrue(vm.content.cart.isEmpty)
        vm.onIntent(LifecycleAction.ResetDemoClicked)
        advanceUntilIdle()
        assertEquals(2, vm.content.cart.lines.single().quantity)
        assertEquals("", vm.content.promo.input)
        assertTrue(vm.content.placeOrder.enabled)
    }

    @Test fun `a completed order cannot be submitted again before navigation`() {
        val completed = reducer.reduce(readyState().copy(submission = Submission.Submitting), OrderAction.Placed(com.example.feature.checkout.domain.model.Order("paid"))).state
        assertNull(completed.orderRequestOrNull())
        assertTrue(reducer.reduce(completed, OrderAction.PlaceClicked).effects.isEmpty())
    }
}
