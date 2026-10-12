package com.example.feature.checkout.reducer

import com.example.core.mvi.Reducer
import com.example.core.mvi.Transition
import com.example.core.mvi.only
import com.example.core.mvi.then
import com.example.core.mvi.withEffects
import com.example.feature.checkout.contract.AddressSlice
import com.example.feature.checkout.contract.CheckoutEffect
import com.example.feature.checkout.contract.CheckoutEvent
import com.example.feature.checkout.contract.CheckoutState
import com.example.feature.checkout.contract.Loadable
import com.example.feature.checkout.contract.PromoStatus
import com.example.feature.checkout.contract.ShippingSlice
import com.example.feature.checkout.contract.availableShippingOptions
import com.example.feature.checkout.contract.contextOrNull

typealias CheckoutTransition = Transition<CheckoutState, CheckoutEffect, CheckoutEvent>
typealias CheckoutSliceReducer<A> = Reducer<CheckoutState, A, CheckoutEffect, CheckoutEvent>

// Cross-slice business rules, shared by several slice reducers.
// Each one is a pure (State) -> Transition step, composable with `then`.

/** Picking an address invalidates validation and the shipping choice, then re-quotes. */
internal fun CheckoutState.selectAddress(addressId: String): CheckoutTransition {
    val validating: CheckoutTransition = copy(
        address = AddressSlice(selectedId = addressId, validation = Loadable.Loading),
        shipping = ShippingSlice(),
    ).withEffects(CheckoutEffect.ValidateAddress(addressId))
    return validating.then { it.requestQuotes() }
}

/** Ask for fresh quotes for the current address + cart, keeping the current pick if possible. */
internal fun CheckoutState.requestQuotes(): CheckoutTransition {
    val addressId = address.selectedId
    val cart = contextOrNull?.cart
    if (addressId == null || cart == null || cart.isEmpty) {
        return copy(shipping = ShippingSlice()).only()
    }
    return copy(shipping = shipping.copy(quotes = Loadable.Loading))
        .withEffects(CheckoutEffect.LoadShippingQuotes(addressId, cart))
}

/** A discount depends on the cart; when the cart changes, an applied promo must be re-checked. */
internal fun CheckoutState.revalidatePromo(): CheckoutTransition {
    val applied = (promo.status as? PromoStatus.Applied)?.promo ?: return only()
    val cart = contextOrNull?.cart ?: return only()
    return copy(promo = promo.copy(status = PromoStatus.Redeeming(applied.code)))
        .withEffects(CheckoutEffect.RedeemPromo(applied.code, cart))
}

/** Keep the chosen shipping option if it is still offered, otherwise default to the cheapest. */
internal fun CheckoutState.reconcileShippingSelection(): CheckoutTransition {
    if (shipping.quotes !is Loadable.Ready) return only()
    val options = availableShippingOptions
    val keep = options.firstOrNull { it.id == shipping.selectedId }
    val next = keep ?: options.minByOrNull { it.price }
    return copy(shipping = shipping.copy(selectedId = next?.id)).only()
}
