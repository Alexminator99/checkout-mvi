package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.Reducer
import com.example.core.mvi.only
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutIntent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import com.example.feature.checkout.presentation.contract.isPlacingOrder
import javax.inject.Inject

/**
 * Routes each action group to the reducer of its section, and owns the one screen-wide rule: while an
 * order is being placed, intents are ignored. A section added by following the README's recipe is
 * therefore frozen during payment without any code of its own.
 *
 * The `when` over the sealed action groups is exhaustive, so a new section does not compile until it
 * is routed here: new behaviour lands in a new slice reducer, and this is the one line to add.
 */
internal class CheckoutReducer @Inject constructor(
    private val lifecycle: LifecycleReducer,
    private val context: ContextReducer,
    private val cart: CartReducer,
    private val address: AddressReducer,
    private val shipping: ShippingReducer,
    private val promo: PromoReducer,
    private val payment: PaymentReducer,
    private val order: OrderReducer,
) : Reducer<CheckoutState, CheckoutAction, CheckoutEffect, CheckoutEvent> {

    override fun reduce(state: CheckoutState, action: CheckoutAction): CheckoutTransition {
        // While the order is being placed every intent is dropped, so what is paid for is what the screen shows.
        // Effect results are not intents, so the order's answer and context updates still arrive.
        if (state.isPlacingOrder && action is CheckoutIntent) return state.only()
        return when (action) {
            is LifecycleAction -> lifecycle.reduce(state, action)
            is ContextAction -> context.reduce(state, action)
            is CartAction -> cart.reduce(state, action)
            is AddressAction -> address.reduce(state, action)
            is ShippingAction -> shipping.reduce(state, action)
            is PromoAction -> promo.reduce(state, action)
            is PaymentAction -> payment.reduce(state, action)
            is OrderAction -> order.reduce(state, action)
        }
    }
}
