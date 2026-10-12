package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.feature.checkout.presentation.contract.AddressAction
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.contextOrNull
import javax.inject.Inject

internal class AddressReducer @Inject constructor() : CheckoutSliceReducer<AddressAction> {

    override fun reduce(state: CheckoutState, action: AddressAction): CheckoutTransition = when (action) {
        is AddressAction.Selected -> {
            val known = state.contextOrNull?.addresses?.any { it.id == action.addressId } == true
            val alreadyChecked = action.addressId == state.address.selectedId &&
                state.address.validation !is Loadable.Failed
            if (!known || alreadyChecked) state.only() else state.selectAddress(action.addressId)
        }

        // Results carry the address they were computed for, so a late answer for a previous address is ignored.
        is AddressAction.Validated ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(address = state.address.copy(validation = Loadable.Ready(action.result))).only()

        is AddressAction.ValidationFailed ->
            if (action.addressId != state.address.selectedId) state.only()
            else state.copy(address = state.address.copy(validation = Loadable.Failed(action.error))).only()
    }
}
