package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderReducerTest {

    private val reducer = OrderReducer(ContextReducer())

    @Test
    fun `paying sends exactly the order the customer sees`() {
        val transition = reducer.reduce(readyState(), OrderAction.PlaceClicked)

        assertEquals(Submission.Submitting, transition.state.submission)
        assertEquals(
            listOf(CheckoutEffect.PlaceOrder(OrderRequest(CHECKOUT_ID, HOME.id, STANDARD.id, VISA.id, promoCode = null))),
            transition.effects,
        )
    }

    @Test
    fun `a second tap while the order is being placed is ignored`() {
        val submitting = reducer.reduce(readyState(), OrderAction.PlaceClicked).state

        val transition = reducer.reduce(submitting, OrderAction.PlaceClicked)

        assertEquals(submitting, transition.state)
        assertTrue(transition.effects.isEmpty())
    }

    @Test
    fun `paying is ignored until the address is confirmed`() {
        val checking = readyState().copy(address = AddressSlice(HOME.id, Loadable.Loading))

        assertTrue(reducer.reduce(checking, OrderAction.PlaceClicked).effects.isEmpty())
    }

    @Test
    fun `paying is ignored while offline`() {
        assertTrue(reducer.reduce(readyState(checkoutContext(isOnline = false)), OrderAction.PlaceClicked).effects.isEmpty())
    }

    @Test
    fun `a placed order navigates once`() {
        val submitting = readyState().copy(submission = Submission.Submitting)

        val transition = reducer.reduce(submitting, OrderAction.Placed(Order("o-42")))

        assertEquals(Submission.Completed, transition.state.submission)
        assertEquals(listOf(CheckoutEvent.NavigateToConfirmation("o-42")), transition.events)
    }

    @Test
    fun `a failed order is shown until dismissed`() {
        val submitting = readyState().copy(submission = Submission.Submitting)

        val failed = reducer.reduce(submitting, OrderAction.Failed(CheckoutError.Payment.Declined)).state

        assertEquals(Submission.Failed(CheckoutError.Payment.Declined), failed.submission)
        assertEquals(Submission.Idle, reducer.reduce(failed, OrderAction.ErrorDismissed).state.submission)
    }

    @Test
    fun `a price change re-quotes shipping so the customer can review the total`() {
        val submitting = readyState().copy(submission = Submission.Submitting)

        val transition = reducer.reduce(submitting, OrderAction.Failed(CheckoutError.PriceChanged))

        assertEquals(listOf(CheckoutEffect.RefreshCart), transition.effects)
    }
}
