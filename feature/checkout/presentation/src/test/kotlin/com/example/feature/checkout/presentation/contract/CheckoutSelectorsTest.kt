package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.presentation.CHECKOUT_ID
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CheckoutSelectorsTest {

    @Test
    fun `a ready checkout produces exactly the order the customer sees`() {
        assertEquals(OrderRequest(CHECKOUT_ID, HOME.id, STANDARD.id, VISA.id, promoCode = null), readyState().orderRequestOrNull())
    }

    @Test
    fun `an applied promo is sent with the order`() {
        val state = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Applied(SAVE5)))

        assertEquals("SAVE5", state.orderRequestOrNull()?.promoCode)
    }

    @Test
    fun `no order can be placed while offline`() {
        assertNull(readyState(checkoutContext(isOnline = false)).orderRequestOrNull())
    }

    @Test
    fun `no order can be placed with an empty cart`() {
        assertNull(readyState(checkoutContext(cart = cartOf())).orderRequestOrNull())
    }

    @Test
    fun `no order can be placed while one is being placed`() {
        assertNull(readyState().copy(submission = Submission.Submitting).orderRequestOrNull())
    }

    @Test
    fun `no order can be placed while a promo is being checked`() {
        assertNull(readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5"))).orderRequestOrNull())
    }

    @Test
    fun `the address must be confirmed deliverable`() {
        val checking = readyState().copy(address = AddressSlice(HOME.id, Loadable.Loading))
        val refused = readyState().copy(
            address = AddressSlice(
                HOME.id,
                Loadable.Ready(AddressValidation.Undeliverable(AddressValidation.Reason.OUTSIDE_SERVICE_AREA)),
            ),
        )

        assertNull(checking.orderRequestOrNull())
        assertNull(refused.orderRequestOrNull())
    }

    @Test
    fun `an express choice is not valid while express is switched off`() {
        val state = readyState(checkoutContext(isExpressShippingEnabled = false))
            .copy(shipping = ShippingSlice(Loadable.Ready(listOf(STANDARD, EXPRESS)), selectedId = EXPRESS.id))

        assertNull(state.orderRequestOrNull())
    }

    @Test
    fun `the chosen card must still be offered`() {
        assertNull(readyState().copy(selectedPaymentId = "gone").orderRequestOrNull())
    }

    @Test
    fun `available options hide express while it is switched off`() {
        assertEquals(listOf(STANDARD, PICKUP), readyState(checkoutContext(isExpressShippingEnabled = false)).availableShippingOptions)
    }
}
