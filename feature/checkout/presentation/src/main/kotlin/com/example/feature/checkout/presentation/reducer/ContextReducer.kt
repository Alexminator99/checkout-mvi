package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.core.mvi.withEvent
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEvent
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.ContextAction
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.contract.contextOrNull
import com.example.feature.checkout.presentation.contract.isPlacingOrder
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
        if (state.isPlacingOrder) return state.copy(
            context = previous?.let { Loadable.Ready(it.copy(isOnline = context.isOnline)) } ?: state.context,
            deferredContext = context,
        ).only()
        val addressBefore = state.address.selectedId
        val cartChanged = previous != null && previous.cart != context.cart
        val selectedAddressChanged = previous?.addresses?.firstOrNull { it.id == addressBefore } !=
            context.addresses.firstOrNull { it.id == addressBefore }

        val previousAddress = previous?.addresses?.firstOrNull { it.id == addressBefore }
        return state.copy(context = Loadable.Ready(context)).only()
            .then { it.reconcileAddress(context, previousAddress) }
            .then { it.reconcilePayment(context) }
            .then { if (cartChanged) it.onCartChanged(if (selectedAddressChanged) null else addressBefore) else it.only() }
            .then { it.reconcileShippingSelection() }
    }

    /** A newly picked address already asked for quotes; otherwise the same address needs quotes for the new cart. */
    private fun CheckoutState.onCartChanged(addressBefore: String?): CheckoutTransition {
        val requote = if (address.selectedId == addressBefore) requestQuotes() else only()
        return requote.then { it.revalidatePromo() }
    }

    private fun CheckoutState.reconcileAddress(
        context: CheckoutContext,
        previous: Address?,
    ): CheckoutTransition {
        val selected = context.addresses.firstOrNull { it.id == address.selectedId }
        if (selected != null) return if (selected == previous) only() else selectAddress(selected.id)
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
