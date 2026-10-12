package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.TEE
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.checkoutContext
import com.example.feature.checkout.domain.rule.MAX_QUANTITY_PER_LINE
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.TestMoneyFormatter
import com.example.feature.checkout.presentation.contract.Submission
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import com.example.feature.checkout.presentation.ui.model.CartLineUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartSectionProjectorTest {

    private val projector = CartSectionProjector(TestMoneyFormatter)

    @Test
    fun `lines show their totals and the title counts the items`() {
        val cart = projector.project(readyCheckout())

        assertEquals(UiText.plural(R.plurals.checkout_item_count, 2, 2), cart.title)
        assertEquals(
            listOf(CartLineUi(MUG.sku, MUG.name, 2, "€24.00", canDecrement = true, decrementRemovesLine = false, canIncrement = true)),
            cart.lines,
        )
    }

    @Test
    fun `decrementing a single unit removes the line`() {
        val cart = projector.project(readyCheckout(readyState(checkoutContext(cart = cartOf(TEE)))))

        assertTrue(cart.lines.single().decrementRemovesLine)
    }

    @Test
    fun `the plus button stops at the per-line maximum`() {
        val full = cartOf(MUG.copy(quantity = MAX_QUANTITY_PER_LINE))

        assertFalse(projector.project(readyCheckout(readyState(checkoutContext(cart = full)))).lines.single().canIncrement)
    }

    @Test
    fun `lines cannot be edited while the order is being placed`() {
        val line = projector.project(readyCheckout(readyState().copy(submission = Submission.Submitting))).lines.single()

        assertFalse(line.canDecrement)
        assertFalse(line.canIncrement)
    }

    @Test
    fun `an empty cart is flagged`() {
        val cart = projector.project(readyCheckout(readyState(checkoutContext(cart = cartOf()))))

        assertTrue(cart.isEmpty)
        assertTrue(cart.lines.isEmpty())
    }
}
