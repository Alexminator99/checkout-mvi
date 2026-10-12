package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.Loadable
import org.junit.Assert.assertEquals
import org.junit.Test

class LifecycleReducerTest {

    private val reducer = LifecycleReducer()

    @Test
    fun `starting asks to observe the checkout context`() {
        val start = CheckoutState(checkoutId = CHECKOUT_ID)

        val transition = reducer.reduce(start, LifecycleAction.Started)

        assertEquals(start, transition.state)
        assertEquals(listOf(CheckoutEffect.ObserveContext), transition.effects)
    }

    @Test
    fun `retrying shows loading and observes the context again`() {
        val failed = CheckoutState(checkoutId = CHECKOUT_ID, context = Loadable.Failed(CheckoutError.Network))

        val transition = reducer.reduce(failed, LifecycleAction.RetryLoadClicked)

        assertEquals(Loadable.Loading, transition.state.context)
        assertEquals(listOf(CheckoutEffect.ObserveContext), transition.effects)
    }
}
