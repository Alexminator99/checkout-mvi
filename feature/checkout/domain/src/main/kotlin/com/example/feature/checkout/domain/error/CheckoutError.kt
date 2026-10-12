package com.example.feature.checkout.domain.error

/**
 * Failures the checkout understands. The data layer maps exceptions and status codes into these,
 * so nothing above it ever handles a Throwable and state stays comparable.
 */
sealed interface CheckoutError {
    data object Network : CheckoutError
    data object Unauthorized : CheckoutError
    data object NotFound : CheckoutError
    data class Unexpected(val diagnostic: String?) : CheckoutError

    sealed interface Promo : CheckoutError {
        data object Invalid : Promo
        data object Expired : Promo
        data object MinimumNotReached : Promo
    }

    sealed interface Payment : CheckoutError {
        data object Declined : Payment
        data object RequiresAction : Payment
    }

    data object OutOfStock : CheckoutError
    data object PriceChanged : CheckoutError
}
