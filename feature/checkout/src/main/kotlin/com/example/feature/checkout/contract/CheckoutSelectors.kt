package com.example.feature.checkout.contract

import com.example.domain.model.AddressValidation
import com.example.domain.model.CheckoutContext
import com.example.domain.model.OrderRequest
import com.example.domain.model.Promo
import com.example.domain.model.ShippingOption

// Selectors: pure, derived business questions about the state.
// Both reducers (guards) and projectors (enabled flags) call these, so a rule
// like "can the order be placed?" is written exactly once.

val CheckoutState.contextOrNull: CheckoutContext?
    get() = (context as? Loadable.Ready)?.value

/** Quotes the customer is allowed to pick right now (feature flag applied). */
val CheckoutState.availableShippingOptions: List<ShippingOption>
    get() {
        val quotes = (shipping.quotes as? Loadable.Ready)?.value.orEmpty()
        val expressEnabled = contextOrNull?.isExpressShippingEnabled == true
        return quotes.filter { expressEnabled || !it.isExpress }
    }

val CheckoutState.selectedShippingOption: ShippingOption?
    get() = availableShippingOptions.firstOrNull { it.id == shipping.selectedId }

val CheckoutState.appliedPromo: Promo?
    get() = (promo.status as? PromoStatus.Applied)?.promo

val CheckoutState.isAddressDeliverable: Boolean
    get() = (address.validation as? Loadable.Ready)?.value == AddressValidation.Deliverable

/**
 * The one definition of "ready to pay". Returns the request to send, or null.
 * The reducer uses it as the guard for PlaceClicked; the projector uses
 * `!= null` for the button's enabled state. They can never disagree.
 */
fun CheckoutState.orderRequestOrNull(): OrderRequest? {
    val ctx = contextOrNull ?: return null
    if (!ctx.isOnline || ctx.cart.isEmpty) return null
    if (submission == Submission.Submitting) return null
    if (promo.status is PromoStatus.Redeeming) return null     // don't race a promo check

    val addressId = address.selectedId?.takeIf { isAddressDeliverable } ?: return null
    val shippingId = selectedShippingOption?.id ?: return null
    val paymentId = selectedPaymentId?.takeIf { id -> ctx.paymentMethods.any { it.id == id } } ?: return null

    return OrderRequest(addressId, shippingId, paymentId, appliedPromo?.code)
}
