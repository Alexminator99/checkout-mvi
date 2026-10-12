package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.usecase.CalculateOrderTotalsUseCase
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.TestMoneyFormatter
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.SummaryRowUi
import org.junit.Assert.assertEquals
import org.junit.Test

class SummarySectionProjectorTest {

    private val projector = SummarySectionProjector(TestMoneyFormatter, CalculateOrderTotalsUseCase())

    @Test
    fun `shows the subtotal, the shipping and the total`() {
        val summary = projector.project(readyCheckout())

        assertEquals(
            listOf(
                SummaryRowUi(UiText.res(R.string.checkout_subtotal), UiText.Raw("€24.00")),
                SummaryRowUi(UiText.res(R.string.checkout_shipping), UiText.Raw("€4.99")),
            ),
            summary.rows,
        )
        assertEquals("€28.99", summary.total)
    }

    @Test
    fun `free shipping says so`() {
        val summary = projector.project(readyCheckout(readyState(checkoutContext(customer = MEMBER))))

        assertEquals(UiText.res(R.string.checkout_free), summary.rows[1].value)
    }

    @Test
    fun `asks for a shipping choice until one is made`() {
        val summary = projector.project(readyCheckout(readyState().copy(shipping = ShippingSlice())))

        assertEquals(UiText.res(R.string.checkout_shipping_pending), summary.rows[1].value)
    }

    @Test
    fun `an applied promo adds a discount row`() {
        val summary = projector.project(readyCheckout(readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Applied(SAVE5)))))

        assertEquals(
            SummaryRowUi(UiText.res(R.string.checkout_discount), UiText.res(R.string.checkout_discount_amount, "€5.00")),
            summary.rows.last(),
        )
        assertEquals("€23.99", summary.total)
    }
}
