package com.example.feature.checkout.contract

import com.example.core.mvi.KeyedEffect
import com.example.domain.error.DomainError
import com.example.domain.model.Cart
import com.example.domain.model.OrderRequest

/**
 * Side effects the reducer *asks for*. Plain data: easy to assert in tests.
 * Concurrency policy lives here (KeyedEffect = latest wins), decided by the
 * reducer's author, executed generically by the store.
 */
sealed interface CheckoutEffect {

    data object ObserveContext : CheckoutEffect, KeyedEffect {
        override val cancelKey: Any get() = "context"
    }

    data class UpdateQuantity(val sku: String, val quantity: Int) : CheckoutEffect

    data class ValidateAddress(val addressId: String) : CheckoutEffect, KeyedEffect {
        override val cancelKey: Any get() = "address-validation"
    }

    data class LoadShippingQuotes(val addressId: String, val cart: Cart) : CheckoutEffect, KeyedEffect {
        override val cancelKey: Any get() = "shipping-quotes"
    }

    data class RedeemPromo(val code: String, val cart: Cart) : CheckoutEffect, KeyedEffect {
        override val cancelKey: Any get() = "promo"
    }

    data class PlaceOrder(val request: OrderRequest) : CheckoutEffect
}

/** One-off signals. "State is, events happen." */
sealed interface CheckoutEvent {
    data class NavigateToConfirmation(val orderId: String) : CheckoutEvent
    data class ShowTransientError(val error: DomainError) : CheckoutEvent
}
