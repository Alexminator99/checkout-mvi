package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError

/** One-off signals for the UI. State is what the screen shows; events are things that happen once. */
sealed interface CheckoutEvent {
    data class NavigateToConfirmation(val orderId: String) : CheckoutEvent
    data class ShowError(val error: CheckoutError) : CheckoutEvent
}
