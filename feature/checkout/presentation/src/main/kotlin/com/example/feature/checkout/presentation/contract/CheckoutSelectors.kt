package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.CheckoutContext
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.rule.availableFor

// Selectors answer business questions about the state. Reducers use them as guards and projectors
// as display rules, so a rule like "can the order be placed?" is written exactly once.

internal val CheckoutState.contextOrNull: CheckoutContext?
    get() = (context as? Loadable.Ready)?.value

/** The shipping options the customer may pick right now. */
internal val CheckoutState.availableShippingOptions: List<ShippingOption>
    get() {
        val quotes = (shipping.quotes as? Loadable.Ready)?.value.orEmpty()
        return quotes.availableFor(isExpressEnabled = contextOrNull?.isExpressShippingEnabled == true)
    }

internal val CheckoutState.selectedShippingOption: ShippingOption?
    get() = availableShippingOptions.firstOrNull { it.id == shipping.selectedId }

internal val CheckoutState.appliedPromo: Promo?
    get() = (promo.status as? PromoStatus.Applied)?.promo

internal val CheckoutState.isAddressDeliverable: Boolean
    get() = (address.validation as? Loadable.Ready)?.value == AddressValidation.Deliverable

/** While the order is being placed the customer's choices are frozen: what the screen shows is what is paid for. */
internal val CheckoutState.isPlacingOrder: Boolean
    get() = submission == Submission.Submitting

internal val CheckoutState.isCheckoutLocked: Boolean
    get() = isPlacingOrder || submission == Submission.Completed || isResettingDemo || priceRefresh is Loadable.Loading

/**
 * The one definition of "ready to pay": the request to send, or null while anything is missing.
 * The order reducer uses it as its guard and the place-order button projector for its enabled state,
 * so the two can never disagree.
 */
internal fun CheckoutState.orderRequestOrNull(): OrderRequest? {
    val context = contextOrNull ?: return null
    if (!context.isOnline || context.cart.isEmpty) return null
    if (submission != Submission.Idle || isUpdatingCart || priceRefresh is Loadable.Loading || priceRefresh is Loadable.Failed) return null
    if (promo.status is PromoStatus.Redeeming) return null

    val addressId = address.selectedId?.takeIf { isAddressDeliverable } ?: return null
    val shippingOptionId = selectedShippingOption?.id ?: return null
    val paymentMethodId = selectedPaymentId?.takeIf { id -> context.paymentMethods.any { it.id == id } } ?: return null

    return OrderRequest(checkoutId, addressId, shippingOptionId, paymentMethodId, appliedPromo?.code)
}
