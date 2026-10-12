package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.PlaceOrderButtonUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceOrderButtonProjectorTest {

    private val projector = PlaceOrderButtonProjector()

    @Test
    fun `a ready checkout can pay the shown total`() {
        assertEquals(
            PlaceOrderButtonUi(UiText.res(R.string.checkout_pay_amount, "€28.99"), enabled = true, loading = false),
            projector.project(readyState(), total = "€28.99"),
        )
    }

    @Test
    fun `paying is disabled while offline`() {
        assertFalse(projector.project(readyState(checkoutContext(isOnline = false)), total = "€28.99").enabled)
    }

    @Test
    fun `the button spins and is disabled while the order is being placed`() {
        val button = projector.project(readyState().copy(submission = Submission.Submitting), total = "€28.99")

        assertTrue(button.loading)
        assertFalse(button.enabled)
    }
}
