package com.example.feature.checkout.projector

import com.example.core.ui.UiText
import com.example.domain.error.DomainError
import com.example.domain.model.AddressValidation
import com.example.feature.checkout.R

/** Single mapping from business errors to user-facing copy. Used by projectors and the screen. */
internal fun DomainError.toUiText(): UiText = UiText.res(
    when (this) {
        DomainError.Network -> R.string.error_network
        DomainError.Unauthorized -> R.string.error_unauthorized
        DomainError.NotFound -> R.string.error_not_found
        DomainError.Promo.Invalid -> R.string.error_promo_invalid
        DomainError.Promo.Expired -> R.string.error_promo_expired
        DomainError.Promo.MinimumNotReached -> R.string.error_promo_minimum
        DomainError.Payment.Declined -> R.string.error_payment_declined
        DomainError.Payment.RequiresAction -> R.string.error_payment_requires_action
        DomainError.OutOfStock -> R.string.error_out_of_stock
        DomainError.PriceChanged -> R.string.error_price_changed
        is DomainError.Unexpected -> R.string.error_generic
    }
)

internal fun AddressValidation.Reason.toUiText(): UiText = UiText.res(
    when (this) {
        AddressValidation.Reason.OUTSIDE_SERVICE_AREA -> R.string.address_outside_area
        AddressValidation.Reason.INCOMPLETE -> R.string.address_incomplete
        AddressValidation.Reason.PO_BOX_NOT_ALLOWED -> R.string.address_po_box
    }
)
