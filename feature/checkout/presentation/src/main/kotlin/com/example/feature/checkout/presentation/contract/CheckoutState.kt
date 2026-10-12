package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.model.CheckoutContext

/**
 * The single source of truth of the checkout screen.
 *
 * Shaped like the domain: no strings, no formatting, no "is the button enabled" flags. Selectors
 * derive business answers from it and projectors derive what the UI shows. It is organised in
 * slices, one per section of the screen, each owned by one slice reducer.
 */
internal data class CheckoutState(
    val checkoutId: String,
    val context: Loadable<CheckoutContext> = Loadable.Loading,
    val address: AddressSlice = AddressSlice(),
    val shipping: ShippingSlice = ShippingSlice(),
    val promo: PromoSlice = PromoSlice(),
    val selectedPaymentId: String? = null,
    val submission: Submission = Submission.Idle,
)
