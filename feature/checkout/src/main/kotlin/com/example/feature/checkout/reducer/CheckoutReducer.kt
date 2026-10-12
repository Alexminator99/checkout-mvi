package com.example.feature.checkout.reducer

import com.example.core.mvi.Reducer
import com.example.feature.checkout.contract.AddressAction
import com.example.feature.checkout.contract.CartAction
import com.example.feature.checkout.contract.CheckoutAction
import com.example.feature.checkout.contract.CheckoutEffect
import com.example.feature.checkout.contract.CheckoutEvent
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.ContextAction
import com.example.feature.checkout.contract.LifecycleAction
import com.example.feature.checkout.contract.OrderAction
import com.example.feature.checkout.contract.PaymentAction
import com.example.feature.checkout.contract.PromoAction
import com.example.feature.checkout.contract.ShippingAction
import javax.inject.Inject

/**
 * Root reducer: routing only, zero business logic.
 *
 * The `when` over a sealed hierarchy is exhaustive, so adding a new slice is a
 * compile error here until it is routed. That is the Open/Closed trade-off we
 * want: new behavior lands in a new slice reducer, and the compiler points to
 * the single line that must change.
 */
internal class CheckoutReducer @Inject constructor(
    private val lifecycle: CheckoutSliceReducer<LifecycleAction>,
    private val context: CheckoutSliceReducer<ContextAction>,
    private val cart: CheckoutSliceReducer<CartAction>,
    private val address: CheckoutSliceReducer<AddressAction>,
    private val shipping: CheckoutSliceReducer<ShippingAction>,
    private val promo: CheckoutSliceReducer<PromoAction>,
    private val payment: CheckoutSliceReducer<PaymentAction>,
    private val order: CheckoutSliceReducer<OrderAction>,
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

    companion object {
        /** Wiring without DI, for tests and previews. */
        fun create(): CheckoutReducer = CheckoutReducer(
            LifecycleReducer(), ContextReducer(), CartReducer(), AddressReducer(),
            ShippingReducer(), PromoReducer(), PaymentReducer(), OrderReducer(),
        )
    }
}
