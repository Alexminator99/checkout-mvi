package com.example.feature.checkout.presentation.reducer

import com.example.core.mvi.only
import com.example.feature.checkout.domain.rule.preferredFor
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.availableShippingOptions
import com.example.feature.checkout.presentation.contract.contextOrNull

/** Keeps the shipping choice valid for the latest quotes, express flag and customer. */
internal fun CheckoutState.reconcileShippingSelection(): CheckoutTransition {
    if (shipping.quotes !is Loadable.Ready) return only()
    val customer = contextOrNull?.customer ?: return only()
    val preferred = availableShippingOptions.preferredFor(shipping.selectedId, customer)
    return copy(shipping = shipping.copy(selectedId = preferred?.id)).only()
}
