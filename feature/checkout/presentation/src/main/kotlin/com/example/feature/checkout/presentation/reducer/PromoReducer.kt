package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.PromoSlice
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class PromoReducer @Inject constructor() : CheckoutSliceReducer<PromoAction> {

    override fun reduce(state: CheckoutState, action: PromoAction): CheckoutTransition {
        val promo = state.promo
        return when (action) {
            is PromoAction.InputChanged -> state.copy(
                promo = promo.copy(
                    input = action.text,
                    status = if (promo.status is PromoStatus.Rejected) PromoStatus.None else promo.status,
                ),
            ).only()

            PromoAction.ApplyClicked -> {
                val code = promo.input.trim()
                val cart = state.contextOrNull?.cart
                val busy = promo.status is PromoStatus.Redeeming || promo.status is PromoStatus.Applied
                if (code.isEmpty() || cart == null || cart.isEmpty || busy) state.only()
                else state.copy(promo = promo.copy(status = PromoStatus.Redeeming(code)))
                    .withEffects(CheckoutEffect.RedeemPromo(code, cart))
            }

            PromoAction.RemoveClicked -> state.copy(promo = PromoSlice()).only()

            // Answers are accepted only while one is awaited: the customer may have removed the promo meanwhile.
            is PromoAction.Redeemed ->
                if (promo.status !is PromoStatus.Redeeming) state.only()
                else state.copy(promo = promo.copy(status = PromoStatus.Applied(action.promo))).only()

            is PromoAction.Rejected ->
                if (promo.status !is PromoStatus.Redeeming) state.only()
                else state.copy(promo = promo.copy(status = PromoStatus.Rejected(action.error))).only()
        }
    }
}
