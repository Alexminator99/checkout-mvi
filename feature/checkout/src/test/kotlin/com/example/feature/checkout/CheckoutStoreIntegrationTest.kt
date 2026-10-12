package com.example.feature.checkout

import com.example.core.mvi.DefaultStore
import com.example.domain.error.DomainError
import com.example.domain.error.Outcome
import com.example.domain.model.OrderRequest
import com.example.domain.usecase.GetShippingQuotesUseCase
import com.example.domain.usecase.ObserveCheckoutContextUseCase
import com.example.domain.usecase.PlaceOrderUseCase
import com.example.domain.usecase.RedeemPromoUseCase
import com.example.domain.usecase.UpdateCartQuantityUseCase
import com.example.domain.usecase.ValidateAddressUseCase
import com.example.feature.checkout.contract.CartAction
import com.example.feature.checkout.contract.CheckoutEvent
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.LifecycleAction
import com.example.feature.checkout.contract.Loadable
import com.example.feature.checkout.contract.OrderAction
import com.example.feature.checkout.contract.PromoAction
import com.example.feature.checkout.contract.PromoStatus
import com.example.feature.checkout.effect.CheckoutEffectHandler
import com.example.feature.checkout.fixtures.FakePorts
import com.example.feature.checkout.fixtures.HOME
import com.example.feature.checkout.fixtures.MUG
import com.example.feature.checkout.fixtures.SAVE5
import com.example.feature.checkout.fixtures.STANDARD
import com.example.feature.checkout.fixtures.VISA
import com.example.feature.checkout.reducer.CheckoutReducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The whole MVI loop on the JVM: real store, real reducer, real effect handler,
 * real use cases; only the ports at the edge are fakes. No Android, no Hilt.
 */
class CheckoutStoreIntegrationTest {

    private fun store(ports: FakePorts, scope: CoroutineScope) = DefaultStore(
        initialState = CheckoutState(),
        scope = scope,
        reducer = CheckoutReducer.create(),
        effectHandler = CheckoutEffectHandler(
            observeContext = ObserveCheckoutContextUseCase(ports, ports, ports, ports, ports, ports),
            updateQuantity = UpdateCartQuantityUseCase(ports),
            validateAddress = ValidateAddressUseCase(ports),
            getShippingQuotes = GetShippingQuotesUseCase(ports),
            redeemPromo = RedeemPromoUseCase(ports),
            placeOrder = PlaceOrderUseCase(ports),
        ),
    )

    @Test
    fun `starting loads context, validates default address and picks cheapest quote`() = runTest {
        val ports = FakePorts()
        val store = store(ports, backgroundScope)

        store.dispatch(LifecycleAction.Started)
        advanceUntilIdle()

        val state = store.state.value
        assertTrue(state.context is Loadable.Ready)
        assertEquals(HOME.id, state.address.selectedId)
        assertEquals(VISA.id, state.selectedPaymentId)
        assertEquals(STANDARD.id, state.shipping.selectedId)
    }

    @Test
    fun `changing quantity flows through the cart port and triggers a re-quote`() = runTest {
        val ports = FakePorts()
        val store = store(ports, backgroundScope)
        store.dispatch(LifecycleAction.Started)
        advanceUntilIdle()

        store.dispatch(CartAction.QuantityChanged(MUG.sku, 3))
        advanceUntilIdle()

        assertEquals(3, ports.cart.value.lines.single().quantity)
        assertEquals(listOf(HOME.id, HOME.id), ports.quoteRequests)   // initial + after cart change
    }

    @Test
    fun `applying a promo and paying sends the promo code with the order and navigates`() = runTest {
        val ports = FakePorts()
        val store = store(ports, backgroundScope)
        val navigation = mutableListOf<CheckoutEvent>()
        backgroundScope.launch { store.events.collect { navigation += it } }

        store.dispatch(LifecycleAction.Started)
        advanceUntilIdle()
        store.dispatch(PromoAction.InputChanged(" save5 "))
        store.dispatch(PromoAction.ApplyClicked)
        advanceUntilIdle()
        assertEquals(PromoStatus.Applied(SAVE5), store.state.value.promo.status)

        store.dispatch(OrderAction.PlaceClicked)
        advanceUntilIdle()

        assertEquals(listOf(OrderRequest(HOME.id, STANDARD.id, VISA.id, "SAVE5")), ports.placedOrders)
        assertEquals(listOf(CheckoutEvent.NavigateToConfirmation("order-1")), navigation)
    }

    @Test
    fun `a failed address fetch on first load shows the error state, and retry recovers`() = runTest {
        val ports = FakePorts(addresses = Outcome.Failure(DomainError.Network))
        val store = store(ports, backgroundScope)

        store.dispatch(LifecycleAction.Started)
        advanceUntilIdle()
        assertEquals(Loadable.Failed(DomainError.Network), store.state.value.context)

        ports.addresses = Outcome.Success(listOf(HOME))
        store.dispatch(LifecycleAction.RetryLoadClicked)
        advanceUntilIdle()

        assertTrue(store.state.value.context is Loadable.Ready)
    }
}
