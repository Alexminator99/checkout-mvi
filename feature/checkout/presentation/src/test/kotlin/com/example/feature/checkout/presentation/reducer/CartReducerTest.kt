package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CartReducerTest {

    private val reducer = CartReducer()

    @Test
    fun `changing a quantity asks to write it and waits for the cart to change`() {
        val transition = reducer.reduce(readyState(), CartAction.QuantityChanged(MUG.sku, 3))

        assertEquals(readyState(), transition.state)
        assertEquals(listOf(CheckoutEffect.UpdateQuantity(MUG.sku, 3)), transition.effects)
    }

    @Test
    fun `the same quantity or an unknown line changes nothing`() {
        assertTrue(reducer.reduce(readyState(), CartAction.QuantityChanged(MUG.sku, MUG.quantity)).effects.isEmpty())
        assertTrue(reducer.reduce(readyState(), CartAction.QuantityChanged("unknown", 1)).effects.isEmpty())
    }

    @Test
    fun `quantities cannot change while the order is being placed`() {
        val submitting = readyState().copy(submission = Submission.Submitting)

        assertTrue(reducer.reduce(submitting, CartAction.QuantityChanged(MUG.sku, 3)).effects.isEmpty())
    }

    @Test
    fun `a failed write is reported once`() {
        val transition = reducer.reduce(readyState(), CartAction.UpdateFailed(CheckoutError.Network))

        assertEquals(listOf(CheckoutEvent.ShowError(CheckoutError.Network)), transition.events)
    }
}
