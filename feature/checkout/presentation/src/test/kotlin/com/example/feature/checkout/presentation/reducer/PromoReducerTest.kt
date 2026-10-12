package com.example.feature.checkout.presentation.reducer

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.SAVE5
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.appliedPromo
import com.example.feature.checkout.presentation.readyState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PromoReducerTest {

    private val reducer = PromoReducer()

    @Test
    fun `applying sends the trimmed code and waits for the answer`() {
        val typed = readyState().copy(promo = PromoSlice(input = " save5 "))

        val transition = reducer.reduce(typed, PromoAction.ApplyClicked)

        assertEquals(PromoStatus.Redeeming("save5"), transition.state.promo.status)
        assertEquals(listOf(CheckoutEffect.RedeemPromo("save5", cartOf(MUG))), transition.effects)
    }

    @Test
    fun `applying is ignored without a code, or while a promo is checked or applied`() {
        val redeeming = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5")))
        val applied = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Applied(SAVE5)))

        assertTrue(reducer.reduce(readyState(), PromoAction.ApplyClicked).effects.isEmpty())
        assertTrue(reducer.reduce(redeeming, PromoAction.ApplyClicked).effects.isEmpty())
        assertTrue(reducer.reduce(applied, PromoAction.ApplyClicked).effects.isEmpty())
    }

    @Test
    fun `typing after a rejected code clears the error`() {
        val rejected = readyState().copy(promo = PromoSlice("BAD", PromoStatus.Rejected(CheckoutError.Promo.Invalid)))

        val transition = reducer.reduce(rejected, PromoAction.InputChanged("BADD"))

        assertEquals(PromoSlice("BADD", PromoStatus.None), transition.state.promo)
    }

    @Test
    fun `a redeemed promo is applied`() {
        val redeeming = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5")))

        assertEquals(PromoStatus.Applied(SAVE5), reducer.reduce(redeeming, PromoAction.Redeemed(SAVE5)).state.promo.status)
    }

    @Test
    fun `an answer arriving after the promo was removed is ignored`() {
        val redeeming = readyState().copy(promo = PromoSlice("SAVE5", PromoStatus.Redeeming("SAVE5")))
        val removed = reducer.reduce(redeeming, PromoAction.RemoveClicked).state

        assertEquals(PromoSlice(), reducer.reduce(removed, PromoAction.Redeemed(SAVE5)).state.promo)
    }

    @Test
    fun `a promo that no longer qualifies after a cart change stops discounting`() {
        // After a cart change the applied BIG50 is checked again, and the smaller order no longer qualifies.
        val rechecking = readyState().copy(promo = PromoSlice("BIG50", PromoStatus.Redeeming("BIG50")))

        val transition = reducer.reduce(rechecking, PromoAction.Rejected(CheckoutError.Promo.MinimumNotReached))

        assertEquals(PromoStatus.Rejected(CheckoutError.Promo.MinimumNotReached), transition.state.promo.status)
        assertNull(transition.state.appliedPromo)
    }
}
