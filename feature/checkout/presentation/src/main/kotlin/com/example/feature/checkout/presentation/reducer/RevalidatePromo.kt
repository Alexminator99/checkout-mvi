package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.appliedPromo
import com.example.feature.checkout.presentation.contract.contextOrNull

/** A discount depends on the cart, so an applied promo is checked again whenever the cart changes. */
internal fun CheckoutState.revalidatePromo(): CheckoutTransition {
    val applied = appliedPromo ?: return only()
    val cart = contextOrNull?.cart ?: return only()
    return copy(promo = promo.copy(status = PromoStatus.Redeeming(applied.code)))
        .withEffects(CheckoutEffect.RedeemPromo(applied.code, cart))
}
