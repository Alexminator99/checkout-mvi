package com.example.feature.checkout.projector

import com.example.core.ui.UiText
import com.example.domain.error.DomainError
import com.example.domain.usecase.CalculateOrderTotalsUseCase
import com.example.feature.checkout.R
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.Loadable
import com.example.feature.checkout.contract.Submission
import com.example.feature.checkout.contract.orderRequestOrNull
import com.example.feature.checkout.fixtures.EXPRESS
import com.example.feature.checkout.fixtures.STANDARD
import com.example.feature.checkout.fixtures.TestMoney
import com.example.feature.checkout.fixtures.context
import com.example.feature.checkout.fixtures.readyState
import com.example.feature.checkout.ui.model.CheckoutUiState
import com.example.feature.checkout.ui.model.ShippingSectionUi
import com.example.feature.checkout.ui.model.SummaryRowUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Projectors are pure too: assert on UiText values, no Context or Robolectric needed. */
class CheckoutProjectorTest {

    private val projector = CheckoutProjector(
        cart = CartSectionProjector(TestMoney),
        address = AddressSectionProjector(),
        shipping = ShippingSectionProjector(TestMoney),
        promo = PromoSectionProjector(TestMoney),
        payment = PaymentSectionProjector(),
        summary = SummaryProjector(TestMoney, CalculateOrderTotalsUseCase()),
    )

    private fun content(state: CheckoutState) = projector.project(state) as CheckoutUiState.Content

    @Test
    fun `loading and error contexts map to full-screen states`() {
        assertEquals(CheckoutUiState.Loading, projector.project(CheckoutState()))
        assertEquals(
            CheckoutUiState.Error(UiText.res(R.string.error_network), canRetry = true),
            projector.project(CheckoutState(context = Loadable.Failed(DomainError.Network))),
        )
    }

    @Test
    fun `pay button uses the same readiness rule as the reducer`() {
        val ready = readyState()
        val notReady = ready.copy(selectedPaymentId = null)

        assertTrue(content(ready).placeOrder.enabled)
        assertEquals(ready.orderRequestOrNull() != null, content(ready).placeOrder.enabled)
        assertFalse(content(notReady).placeOrder.enabled)
    }

    @Test
    fun `pay button shows the formatted total`() {
        // 2 x 12.00 mugs + 4.99 standard shipping
        assertEquals(UiText.res(R.string.checkout_pay_amount, "€28.99"), content(readyState()).placeOrder.label)
    }

    @Test
    fun `members see standard shipping as free, but still pay for express`() {
        val ui = content(readyState(context(premium = true))).shipping as ShippingSectionUi.Options

        assertEquals(UiText.res(R.string.checkout_free_for_members), ui.rows.first { it.id == STANDARD.id }.trailing)
        assertEquals(UiText.Raw("€12.99"), ui.rows.first { it.id == EXPRESS.id }.trailing)
    }

    @Test
    fun `express options are hidden when the flag is off`() {
        val ui = content(readyState(context(express = false))).shipping as ShippingSectionUi.Options

        assertEquals(listOf(STANDARD.id), ui.rows.map { it.id })
    }

    @Test
    fun `summary shows free shipping for members`() {
        val rows = content(readyState(context(premium = true))).summary.rows

        assertEquals(SummaryRowUi(UiText.res(R.string.checkout_shipping), UiText.res(R.string.checkout_free)), rows[1])
    }

    @Test
    fun `a failed submission becomes a blocking error dialog`() {
        val state = readyState().copy(submission = Submission.Failed(DomainError.Payment.Declined))

        assertEquals(UiText.res(R.string.error_payment_declined), content(state).blockingError)
    }
}
