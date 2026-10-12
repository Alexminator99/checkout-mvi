package com.example.feature.checkout.contract

import com.example.domain.error.DomainError
import com.example.domain.model.AddressValidation
import com.example.domain.model.CheckoutContext
import com.example.domain.model.Promo
import com.example.domain.model.ShippingOption

/**
 * The single source of truth for the screen. Domain-shaped, not UI-shaped:
 * no strings, no formatting, no "isButtonEnabled" flags. Those are derived by
 * selectors (business questions) and projectors (presentation).
 *
 * Organized in slices so each slice reducer owns a clearly bounded part.
 */
data class CheckoutState(
    val context: Loadable<CheckoutContext> = Loadable.Loading,
    val address: AddressSlice = AddressSlice(),
    val shipping: ShippingSlice = ShippingSlice(),
    val promo: PromoSlice = PromoSlice(),
    val selectedPaymentId: String? = null,
    val submission: Submission = Submission.Idle,
)

data class AddressSlice(
    val selectedId: String? = null,
    val validation: Loadable<AddressValidation> = Loadable.Idle,
)

data class ShippingSlice(
    val quotes: Loadable<List<ShippingOption>> = Loadable.Idle,
    val selectedId: String? = null,
)

data class PromoSlice(
    val input: String = "",
    val status: PromoStatus = PromoStatus.None,
)

sealed interface PromoStatus {
    data object None : PromoStatus
    data class Redeeming(val code: String) : PromoStatus
    data class Applied(val promo: Promo) : PromoStatus
    data class Rejected(val error: DomainError) : PromoStatus
}

sealed interface Submission {
    data object Idle : Submission
    data object Submitting : Submission
    data class Failed(val error: DomainError) : Submission
}

sealed interface Loadable<out T> {
    data object Idle : Loadable<Nothing>
    data object Loading : Loadable<Nothing>
    data class Ready<T>(val value: T) : Loadable<T>
    data class Failed(val error: DomainError) : Loadable<Nothing>
}
