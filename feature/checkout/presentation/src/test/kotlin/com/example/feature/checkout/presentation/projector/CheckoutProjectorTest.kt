package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.checkoutProjector
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.CheckoutUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckoutProjectorTest {

    private val projector = checkoutProjector()

    private fun content(state: CheckoutState) = projector.project(state) as CheckoutUiState.Content

    @Test
    fun `a loading context shows the loading screen`() {
        assertEquals(CheckoutUiState.Loading, projector.project(CheckoutState(checkoutId = CHECKOUT_ID)))
    }

    @Test
    fun `a failed context shows a full-screen error`() {
        val failed = CheckoutState(checkoutId = CHECKOUT_ID, context = Loadable.Failed(CheckoutError.Network))

        assertEquals(CheckoutUiState.Error(UiText.res(R.string.error_network)), projector.project(failed))
    }

    @Test
    fun `being offline shows the banner`() {
        assertTrue(content(readyState(checkoutContext(isOnline = false))).isOffline)
        assertFalse(content(readyState()).isOffline)
    }

    @Test
    fun `the pay button shows the total`() {
        // 2 x 12.00 mugs + 4.99 standard shipping
        assertEquals(UiText.res(R.string.checkout_pay_amount, "€28.99"), content(readyState()).placeOrder.label)
    }

    @Test
    fun `a failed order becomes a blocking error`() {
        val state = readyState().copy(submission = Submission.Failed(CheckoutError.Payment.Declined))

        assertEquals(UiText.res(R.string.error_payment_declined), content(state).blockingError)
    }
}
