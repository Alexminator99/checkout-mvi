package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.core.mvi.withEffects
import com.example.feature.checkout.presentation.contract.CheckoutEffect
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.ShippingSlice
import com.example.feature.checkout.presentation.contract.contextOrNull

/** Asks for quotes for the chosen address and the current cart. Without both there is nothing to quote. */
internal fun CheckoutState.requestQuotes(): CheckoutTransition {
    val addressId = address.selectedId
    val cart = contextOrNull?.cart
    if (addressId == null || cart == null || cart.isEmpty) return copy(shipping = ShippingSlice()).only()
    return copy(shipping = shipping.copy(quotes = Loadable.Loading))
        .withEffects(CheckoutEffect.LoadShippingQuotes(addressId, cart))
}
