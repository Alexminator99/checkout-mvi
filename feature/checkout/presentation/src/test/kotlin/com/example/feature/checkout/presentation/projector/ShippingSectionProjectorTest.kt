package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MEMBER
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.TestMoneyFormatter
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingSectionProjectorTest {

    private val projector = ShippingSectionProjector(TestMoneyFormatter)

    private fun options(state: CheckoutState) = projector.project(readyCheckout(state)) as ShippingSectionUi.Options

    private fun section(shipping: ShippingSlice) = projector.project(readyCheckout(readyState().copy(shipping = shipping)))

    @Test
    fun `regular customers see each price and delivery time`() {
        val standard = options(readyState()).rows.first { it.id == STANDARD.id }

        assertEquals(UiText.Raw("€4.99"), standard.trailing)
        assertEquals(UiText.plural(R.plurals.checkout_delivery_days, 3, 3), standard.subtitle)
    }

    @Test
    fun `members see standard shipping as free but still pay for express`() {
        val rows = options(readyState(checkoutContext(customer = MEMBER))).rows

        assertEquals(UiText.res(R.string.checkout_free_for_members), rows.first { it.id == STANDARD.id }.trailing)
        assertEquals(UiText.Raw("€12.99"), rows.first { it.id == EXPRESS.id }.trailing)
    }

    @Test
    fun `express options are hidden while the flag is off`() {
        val rows = options(readyState(checkoutContext(isExpressShippingEnabled = false))).rows

        assertEquals(listOf(STANDARD.id, PICKUP.id), rows.map { it.id })
    }

    @Test
    fun `waiting, failed and missing quotes each have their own state`() {
        assertEquals(ShippingSectionUi.Loading, section(ShippingSlice(Loadable.Loading)))
        assertEquals(ShippingSectionUi.Error(UiText.res(R.string.error_network)), section(ShippingSlice(Loadable.Failed(CheckoutError.Network))))
        assertEquals(ShippingSectionUi.Hidden, section(ShippingSlice()))
    }
}
