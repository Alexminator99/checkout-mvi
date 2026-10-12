package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.then
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.AddressSlice
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice

/** Picking an address restarts its check, forgets the shipping choice and asks for new quotes. */
internal fun CheckoutState.selectAddress(addressId: String): CheckoutTransition {
    val checking: CheckoutTransition = copy(
        address = AddressSlice(selectedId = addressId, validation = Loadable.Loading),
        shipping = ShippingSlice(),
    ).withEffects(CheckoutEffect.ValidateAddress(addressId))
    return checking.then { it.requestQuotes() }
}
