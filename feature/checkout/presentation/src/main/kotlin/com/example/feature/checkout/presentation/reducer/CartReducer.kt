package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.core.mvi.withEvent
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class CartReducer @Inject constructor(private val context: ContextReducer) : CheckoutSliceReducer<CartAction> {
    override fun reduce(state: CheckoutState, action: CartAction): CheckoutTransition = when (action) {
        is CartAction.QuantityChanged -> onQuantityChanged(state, action)
        is CartAction.Updated -> {
            val current = state.contextOrNull
            val settled = state.copy(isUpdatingCart = false)
            if (current == null) settled.only()
            else context.reduce(settled, ContextAction.Updated(current.copy(cart = action.cart)))
        }
        is CartAction.UpdateFailed -> state.copy(isUpdatingCart = false).withEvent(CheckoutEvent.ShowError(action.error))
    }

    private fun onQuantityChanged(state: CheckoutState, action: CartAction.QuantityChanged): CheckoutTransition {
        val line = state.contextOrNull?.cart?.lines?.firstOrNull { it.sku == action.sku }
        val unchanged = line == null || line.quantity == action.quantity
        return if (unchanged || state.isUpdatingCart) state.only()
        else state.copy(isUpdatingCart = true).withEffects(CheckoutEffect.UpdateQuantity(action.sku, action.quantity))
    }
}
