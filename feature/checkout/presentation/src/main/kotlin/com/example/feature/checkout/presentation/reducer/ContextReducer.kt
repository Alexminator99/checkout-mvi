package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.core.mvi.withEvent
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class ContextReducer @Inject constructor() : CheckoutSliceReducer<ContextAction> {

    override fun reduce(state: CheckoutState, action: ContextAction): CheckoutTransition = when (action) {
        is ContextAction.Updated -> onUpdated(state, action.context)
        is ContextAction.Failed ->
            // Keep showing what is loaded when a refresh fails; only the first load blocks the screen.
            if (state.contextOrNull != null) state.withEvent(CheckoutEvent.ShowError(action.error))
            else state.copy(context = Loadable.Failed(action.error)).only()
    }

    private fun onUpdated(state: CheckoutState, context: CheckoutContext): CheckoutTransition {
        val previous = state.contextOrNull
        val addressBefore = state.address.selectedId
        val cartChanged = previous != null && previous.cart != context.cart

        return state.copy(context = Loadable.Ready(context)).only()
            .then { it.reconcileAddress(context) }
            .then { it.reconcilePayment(context) }
            .then { if (cartChanged) it.onCartChanged(addressBefore) else it.only() }
            .then { it.reconcileShippingSelection() }
    }

    /** A newly picked address already asked for quotes; otherwise the same address needs quotes for the new cart. */
    private fun CheckoutState.onCartChanged(addressBefore: String?): CheckoutTransition {
        val requote = if (address.selectedId == addressBefore) requestQuotes() else only()
        return requote.then { it.revalidatePromo() }
    }

    private fun CheckoutState.reconcileAddress(context: CheckoutContext): CheckoutTransition {
        if (context.addresses.any { it.id == address.selectedId }) return only()
        val fallback = context.addresses.firstOrNull { it.isDefault } ?: context.addresses.firstOrNull()
        return if (fallback != null) selectAddress(fallback.id)
        else copy(address = AddressSlice(), shipping = ShippingSlice()).only()
    }

    private fun CheckoutState.reconcilePayment(context: CheckoutContext): CheckoutTransition {
        if (context.paymentMethods.any { it.id == selectedPaymentId }) return only()
        val fallback = context.paymentMethods.firstOrNull { it.isDefault } ?: context.paymentMethods.firstOrNull()
        return copy(selectedPaymentId = fallback?.id).only()
    }
}
