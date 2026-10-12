package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PaymentSectionProjectorTest {

    @Test
    fun `each card shows its brand and last digits, with the selection marked`() {
        val rows = PaymentSectionProjector().project(readyCheckout()).rows

        assertEquals(
            SelectableRowUi(VISA.id, UiText.res(R.string.checkout_card_label, "Visa", "4242"), subtitle = null, trailing = null, selected = true, enabled = true),
            rows.first(),
        )
        assertFalse(rows.first { it.id == MASTERCARD.id }.selected)
    }

    @Test
    fun `no card can be picked while the order is being placed`() {
        val rows = PaymentSectionProjector().project(readyCheckout(readyState().copy(submission = Submission.Submitting))).rows

        assertEquals(setOf(false), rows.map { it.enabled }.toSet())
    }
}
