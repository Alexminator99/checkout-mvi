package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.TestMoneyFormatter
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.readyCheckout
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromoSectionProjectorTest {

    private val projector = PromoSectionProjector(TestMoneyFormatter)

    private fun promo(slice: PromoSlice) = projector.project(readyCheckout(readyState().copy(promo = slice)))

    @Test
    fun `a typed code can be applied`() {
        val ui = promo(PromoSlice(input = "SAVE5"))

        assertTrue(ui.inputEnabled)
        assertTrue(ui.applyEnabled)
    }

    @Test
    fun `a blank code cannot be applied`() {
        assertFalse(promo(PromoSlice(input = " ")).applyEnabled)
    }

    @Test
    fun `checking a code locks the field`() {
        val ui = promo(PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5")))

        assertFalse(ui.inputEnabled)
        assertTrue(ui.isRedeeming)
    }

    @Test
    fun `an applied promo shows its discount`() {
        assertEquals(
            UiText.res(R.string.checkout_promo_applied, "SAVE5", "€5.00"),
            promo(PromoSlice("SAVE5", PromoStatus.Applied(SAVE5))).applied,
        )
    }

    @Test
    fun `a rejected code explains why and can be edited`() {
        val ui = promo(PromoSlice("OLD", PromoStatus.Rejected(CheckoutError.Promo.Expired)))

        assertEquals(UiText.res(R.string.error_promo_expired), ui.error)
        assertTrue(ui.inputEnabled)
    }
}
