package com.example.feature.checkout.reducer

import com.example.domain.error.DomainError
import com.example.domain.model.AddressValidation
import com.example.domain.model.Order
import com.example.domain.model.OrderRequest
import com.example.feature.checkout.contract.AddressAction
import com.example.feature.checkout.contract.CheckoutEffect
import com.example.feature.checkout.contract.CheckoutEvent
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.ContextAction
import com.example.feature.checkout.contract.Loadable
import com.example.feature.checkout.contract.OrderAction
import com.example.feature.checkout.contract.PromoAction
import com.example.feature.checkout.contract.PromoSlice
import com.example.feature.checkout.contract.PromoStatus
import com.example.feature.checkout.contract.ShippingAction
import com.example.feature.checkout.contract.Submission
import com.example.feature.checkout.fixtures.EXPRESS
import com.example.feature.checkout.fixtures.HOME
import com.example.feature.checkout.fixtures.MUG
import com.example.feature.checkout.fixtures.OFFICE
import com.example.feature.checkout.fixtures.PICKUP
import com.example.feature.checkout.fixtures.SAVE5
import com.example.feature.checkout.fixtures.STANDARD
import com.example.feature.checkout.fixtures.TEE
import com.example.feature.checkout.fixtures.VISA
import com.example.feature.checkout.fixtures.cartOf
import com.example.feature.checkout.fixtures.context
import com.example.feature.checkout.fixtures.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plain functions in, plain data out: no coroutines, no mocks, no Android.
 * Each test reads as a business rule.
 */
class CheckoutReducerTest {

    private val reducer = CheckoutReducer.create()

    @Test
    fun `first context load picks default address and payment, then validates and quotes`() {
        val ctx = context()

        val t = reducer.reduce(CheckoutState(), ContextAction.Updated(ctx))

        assertEquals(HOME.id, t.state.address.selectedId)
        assertEquals(VISA.id, t.state.selectedPaymentId)
        assertEquals(Loadable.Loading, t.state.shipping.quotes)
        assertEquals(
            listOf(CheckoutEffect.ValidateAddress(HOME.id), CheckoutEffect.LoadShippingQuotes(HOME.id, ctx.cart)),
            t.effects,
        )
    }

    @Test
    fun `cart change re-quotes shipping and re-validates an applied promo`() {
        val start = readyState().copy(promo = PromoSlice(input = "SAVE5", status = PromoStatus.Applied(SAVE5)))
        val newCart = cartOf(MUG, TEE)

        val t = reducer.reduce(start, ContextAction.Updated(context(cart = newCart)))

        assertEquals(
            listOf(CheckoutEffect.LoadShippingQuotes(HOME.id, newCart), CheckoutEffect.RedeemPromo("SAVE5", newCart)),
            t.effects,
        )
        assertEquals(PromoStatus.Redeeming("SAVE5"), t.state.promo.status)
    }

    @Test
    fun `selecting a new address resets shipping and asks for validation plus quotes`() {
        val t = reducer.reduce(readyState(), AddressAction.Selected(OFFICE.id))

        assertNull(t.state.shipping.selectedId)
        assertEquals(Loadable.Loading, t.state.address.validation)
        assertEquals(2, t.effects.size)
    }

    @Test
    fun `late quotes for a previous address are ignored`() {
        val start = reducer.reduce(readyState(), AddressAction.Selected(OFFICE.id)).state

        val t = reducer.reduce(start, ShippingAction.QuotesLoaded(HOME.id, listOf(PICKUP)))

        assertEquals(start, t.state)
    }

    @Test
    fun `loaded quotes keep the current pick if offered, else default to the cheapest`() {
        val start = readyState().copy(shipping = readyState().shipping.copy(quotes = Loadable.Loading, selectedId = "gone"))

        val t = reducer.reduce(start, ShippingAction.QuotesLoaded(HOME.id, listOf(STANDARD, PICKUP, EXPRESS)))

        assertEquals(PICKUP.id, t.state.shipping.selectedId)
    }

    @Test
    fun `turning the express flag off moves an express selection to an allowed option`() {
        val start = readyState().copy(shipping = readyState().shipping.copy(selectedId = EXPRESS.id))

        val t = reducer.reduce(start, ContextAction.Updated(context(express = false)))

        assertEquals(STANDARD.id, t.state.shipping.selectedId)
    }

    @Test
    fun `place order is ignored until the address is confirmed deliverable`() {
        val start = readyState().copy(address = readyState().address.copy(validation = Loadable.Loading))

        val t = reducer.reduce(start, OrderAction.PlaceClicked)

        assertEquals(start, t.state)
        assertTrue(t.effects.isEmpty())
    }

    @Test
    fun `place order is ignored while offline`() {
        val start = readyState(context(online = false))

        assertTrue(reducer.reduce(start, OrderAction.PlaceClicked).effects.isEmpty())
    }

    @Test
    fun `place order submits the exact request when everything is ready`() {
        val t = reducer.reduce(readyState(), OrderAction.PlaceClicked)

        assertEquals(Submission.Submitting, t.state.submission)
        assertEquals(listOf(CheckoutEffect.PlaceOrder(OrderRequest(HOME.id, STANDARD.id, VISA.id, null))), t.effects)
    }

    @Test
    fun `a placed order navigates exactly once via an event`() {
        val t = reducer.reduce(readyState().copy(submission = Submission.Submitting), OrderAction.Placed(Order("o-42")))

        assertEquals(listOf(CheckoutEvent.NavigateToConfirmation("o-42")), t.events)
        assertEquals(Submission.Idle, t.state.submission)
    }

    @Test
    fun `price change on submit re-quotes shipping so the user can review`() {
        val t = reducer.reduce(readyState().copy(submission = Submission.Submitting), OrderAction.Failed(DomainError.PriceChanged))

        assertEquals(Submission.Failed(DomainError.PriceChanged), t.state.submission)
        assertTrue(t.effects.single() is CheckoutEffect.LoadShippingQuotes)
    }

    @Test
    fun `promo result arriving after the user removed the promo is ignored`() {
        val removed = reducer.reduce(
            readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5"))),
            PromoAction.RemoveClicked,
        ).state

        val t = reducer.reduce(removed, PromoAction.Redeemed(SAVE5))

        assertEquals(PromoSlice(), t.state.promo)
    }

    @Test
    fun `typing after a rejected promo clears the error`() {
        val start = readyState().copy(promo = PromoSlice("BAD", PromoStatus.Rejected(DomainError.Promo.Invalid)))

        val t = reducer.reduce(start, PromoAction.InputChanged("BADD"))

        assertEquals(PromoStatus.None, t.state.promo.status)
    }

    @Test
    fun `address validation result for the current address is stored`() {
        val start = reducer.reduce(readyState(), AddressAction.Selected(OFFICE.id)).state
        val undeliverable = AddressValidation.Undeliverable(AddressValidation.Reason.PO_BOX_NOT_ALLOWED)

        val t = reducer.reduce(start, AddressAction.Validated(OFFICE.id, undeliverable))

        assertEquals(Loadable.Ready(undeliverable), t.state.address.validation)
    }
}
