package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.core.mvi.withEvent
import com.example.feature.checkout.presentation.contract.CartAction
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class CartReducer @Inject constructor() : CheckoutSliceReducer<CartAction> {

    override fun reduce(state: CheckoutState, action: CartAction): CheckoutTransition = when (action) {
        is CartAction.QuantityChanged -> onQuantityChanged(state, action)
        is CartAction.UpdateFailed -> state.withEvent(CheckoutEvent.ShowError(action.error))
    }

    // No optimistic update: the cart stream is the source of truth and emits the new cart after the write.
    private fun onQuantityChanged(state: CheckoutState, action: CartAction.QuantityChanged): CheckoutTransition {
        val line = state.contextOrNull?.cart?.lines?.firstOrNull { it.sku == action.sku }
        val unchanged = line == null || line.quantity == action.quantity
        return if (unchanged) state.only()
        else state.withEffects(CheckoutEffect.UpdateQuantity(action.sku, action.quantity))
    }
}
