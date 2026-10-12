package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.presentation.R

/** The single mapping from checkout errors to what the customer reads. */
internal fun CheckoutError.toUiText(): UiText = UiText.res(
    when (this) {
        CheckoutError.Network -> R.string.error_network
        CheckoutError.Unauthorized -> R.string.error_unauthorized
        CheckoutError.NotFound -> R.string.error_not_found
        is CheckoutError.Unexpected -> R.string.error_generic
        CheckoutError.Promo.Invalid -> R.string.error_promo_invalid
        CheckoutError.Promo.Expired -> R.string.error_promo_expired
        CheckoutError.Promo.MinimumNotReached -> R.string.error_promo_minimum
        CheckoutError.Payment.Declined -> R.string.error_payment_declined
        CheckoutError.Payment.RequiresAction -> R.string.error_payment_requires_action
        CheckoutError.OutOfStock -> R.string.error_out_of_stock
        CheckoutError.PriceChanged -> R.string.error_price_changed
    },
)
