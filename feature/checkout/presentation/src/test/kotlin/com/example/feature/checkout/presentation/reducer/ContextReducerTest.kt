package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.TEE
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.contract.orderRequestOrNull
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextReducerTest {

    private val reducer = ContextReducer()

    @Test
    fun `the first context picks the default address and card, then checks the address and quotes`() {
        val context = checkoutContext()

        val transition = reducer.reduce(CheckoutState(checkoutId = CHECKOUT_ID), ContextAction.Updated(context))

        assertEquals(AddressSlice(HOME.id, Loadable.Loading), transition.state.address)
        assertEquals(VISA.id, transition.state.selectedPaymentId)
        assertEquals(Loadable.Loading, transition.state.shipping.quotes)
        assertEquals(
            listOf(CheckoutEffect.ValidateAddress(HOME.id), CheckoutEffect.LoadShippingQuotes(HOME.id, context.cart)),
            transition.effects,
        )
    }

    @Test
    fun `a cart change re-quotes shipping and re-checks an applied promo`() {
        val start = readyState().copy(promo = PromoSlice(input = "SAVE5", status = PromoStatus.Applied(SAVE5)))
        val newCart = cartOf(MUG, TEE)

        val transition = reducer.reduce(start, ContextAction.Updated(checkoutContext(cart = newCart)))

        assertEquals(
            listOf(CheckoutEffect.LoadShippingQuotes(HOME.id, newCart), CheckoutEffect.RedeemPromo("SAVE5", newCart)),
            transition.effects,
        )
        assertEquals(PromoStatus.Redeeming("SAVE5"), transition.state.promo.status)
    }

    @Test
    fun `emptying the cart clears shipping and blocks paying`() {
        val transition = reducer.reduce(readyState(), ContextAction.Updated(checkoutContext(cart = cartOf())))

        assertEquals(ShippingSlice(), transition.state.shipping)
        assertNull(transition.state.orderRequestOrNull())
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun `switching express off moves an express choice to an allowed option`() {
        val start = readyState().copy(shipping = readyState().shipping.copy(selectedId = EXPRESS.id))

        val transition = reducer.reduce(start, ContextAction.Updated(checkoutContext(isExpressShippingEnabled = false)))

        assertEquals(PICKUP.id, transition.state.shipping.selectedId)
    }

    @Test
    fun `a removed address falls back to the default one`() {
        val start = readyState().copy(address = AddressSlice(OFFICE.id, Loadable.Ready(AddressValidation.Deliverable)))

        val transition = reducer.reduce(start, ContextAction.Updated(checkoutContext(addresses = listOf(HOME))))

        assertEquals(HOME.id, transition.state.address.selectedId)
        assertTrue(CheckoutEffect.ValidateAddress(HOME.id) in transition.effects)
    }

    @Test
    fun `a failed first load blocks the screen`() {
        val transition = reducer.reduce(CheckoutState(checkoutId = CHECKOUT_ID), ContextAction.Failed(CheckoutError.Network))

        assertEquals(Loadable.Failed(CheckoutError.Network), transition.state.context)
        assertTrue(transition.events.isEmpty())
    }

    @Test
    fun `a failed refresh keeps the checkout and reports the error once`() {
        val transition = reducer.reduce(readyState(), ContextAction.Failed(CheckoutError.Network))

        assertEquals(readyState(), transition.state)
        assertEquals(listOf(CheckoutEvent.ShowError(CheckoutError.Network)), transition.events)
    }
}
