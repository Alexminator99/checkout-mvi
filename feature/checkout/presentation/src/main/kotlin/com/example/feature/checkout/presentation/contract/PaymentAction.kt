package com.example.feature.checkout.presentation.contract

sealed interface PaymentAction : CheckoutAction {
    data class Selected(val paymentMethodId: String) : PaymentAction, CheckoutIntent
}
