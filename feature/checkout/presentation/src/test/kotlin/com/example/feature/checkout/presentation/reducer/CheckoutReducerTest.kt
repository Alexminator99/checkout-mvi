package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.checkoutReducer
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Test

class CheckoutReducerTest {

    private val reducer = checkoutReducer()

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
}
