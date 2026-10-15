package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.Cart

sealed interface CartAction : CheckoutAction {
    data class QuantityChanged(val sku: String, val quantity: Int) : CartAction, CheckoutIntent
    data class Updated(val cart: Cart) : CartAction
    data class UpdateFailed(val error: CheckoutError) : CartAction
}
