package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.Reducer
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.LifecycleAction
import com.example.feature.checkout.presentation.contract.OrderAction
import com.example.feature.checkout.presentation.contract.PaymentAction
import com.example.feature.checkout.presentation.contract.PromoAction
import com.example.feature.checkout.presentation.contract.ShippingAction
import javax.inject.Inject

/**
 * Routes each action group to the reducer of its section, and does nothing else.
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

    override fun reduce(state: CheckoutState, action: CheckoutAction): CheckoutTransition = when (action) {
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
