package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.contextOrNull

/** A discount depends on the cart, so a promo that is applied or still being checked is checked again whenever the cart changes. */
internal fun CheckoutState.revalidatePromo(): CheckoutTransition {
    val code = when (val status = promo.status) {
        is PromoStatus.Applied -> status.promo.code
        is PromoStatus.Redeeming -> status.code
        PromoStatus.None, is PromoStatus.Rejected -> return only()
    }
    val cart = contextOrNull?.cart ?: return only()
    return copy(promo = promo.copy(status = PromoStatus.Redeeming(code)))
        .withEffects(CheckoutEffect.RedeemPromo(code, cart))
}
