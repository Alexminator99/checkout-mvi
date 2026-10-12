package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingAction
import com.example.feature.checkout.presentation.contract.availableShippingOptions
import javax.inject.Inject

internal class ShippingReducer @Inject constructor() : CheckoutSliceReducer<ShippingAction> {

    override fun reduce(state: CheckoutState, action: ShippingAction): CheckoutTransition = when (action) {
        is ShippingAction.Selected ->
            if (state.availableShippingOptions.none { it.id == action.optionId }) state.only()
            else state.copy(shipping = state.shipping.copy(selectedId = action.optionId)).only()

        ShippingAction.RetryClicked -> state.requestQuotes()

        // Quotes carry the address they were computed for, so quotes for a previous address are ignored.
        is ShippingAction.QuotesLoaded ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(shipping = state.shipping.copy(quotes = Loadable.Ready(action.options))).only()
                .then { it.reconcileShippingSelection() }

        is ShippingAction.QuotesFailed ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(shipping = state.shipping.copy(quotes = Loadable.Failed(action.error))).only()
    }
}
