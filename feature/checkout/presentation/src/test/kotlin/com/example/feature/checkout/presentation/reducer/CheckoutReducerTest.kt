package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.checkoutReducer
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutIntent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutReducerTest {

    private val reducer = checkoutReducer()

    private val placingOrder = readyState().copy(submission = Submission.Submitting)

    @Test
    fun `every action group reaches its slice reducer`() {
        assertEquals(
            listOf(CheckoutEffect.ObserveContext),
            reducer.reduce(CheckoutState(checkoutId = CHECKOUT_ID), LifecycleAction.Started).effects,
        )
        assertEquals(MASTERCARD.id, reducer.reduce(readyState(), PaymentAction.Selected(MASTERCARD.id)).state.selectedPaymentId)
        assertEquals(Submission.Submitting, reducer.reduce(readyState(), OrderAction.PlaceClicked).state.submission)
        assertEquals("SAVE5", reducer.reduce(readyState(), PromoAction.InputChanged("SAVE5")).state.promo.input)
    }

    @Test
    fun `quantities cannot change while the order is being placed`() {
        val transition = reducer.reduce(placingOrder, CartAction.QuantityChanged(MUG.sku, 3))

        assertEquals(placingOrder, transition.state)
        assertTrue(transition.effects.isEmpty())
        assertTrue(transition.events.isEmpty())
    }

    @Test
    fun `every intent is ignored while the order is being placed`() {
        val intents = listOf<CheckoutIntent>(
            LifecycleAction.RetryLoadClicked,
            AddressAction.Selected(OFFICE.id),
            ShippingAction.Selected(PICKUP.id),
            ShippingAction.RetryClicked,
            PaymentAction.Selected(MASTERCARD.id),
            PromoAction.InputChanged("X"),
            PromoAction.ApplyClicked,
            PromoAction.RemoveClicked,
            OrderAction.PlaceClicked,
            OrderAction.ErrorDismissed,
        )

        intents.forEach { intent ->
            val transition = reducer.reduce(placingOrder, intent)

            assertEquals(intent.toString(), placingOrder, transition.state)
            assertTrue(intent.toString(), transition.effects.isEmpty())
            assertTrue(intent.toString(), transition.events.isEmpty())
        }
    }

    @Test
    fun `results still arrive while the order is being placed`() {
        val placed = reducer.reduce(placingOrder, OrderAction.Placed(Order("o-42")))
        assertEquals(listOf(CheckoutEvent.NavigateToConfirmation("o-42")), placed.events)

        val offline = checkoutContext(isOnline = false)
        val updated = reducer.reduce(placingOrder, ContextAction.Updated(offline))
        assertEquals(placingOrder.copy(context = Loadable.Ready(offline)), updated.state)
    }
}
