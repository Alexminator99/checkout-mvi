package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentReducerTest {

    private val reducer = PaymentReducer()

    @Test
    fun `picking an offered card selects it`() {
        assertEquals(MASTERCARD.id, reducer.reduce(readyState(), PaymentAction.Selected(MASTERCARD.id)).state.selectedPaymentId)
    }

    @Test
    fun `picking an unknown card is ignored`() {
        assertEquals(readyState(), reducer.reduce(readyState(), PaymentAction.Selected("gone")).state)
    }
}
