package com.example.feature.checkout.presentation.contract

import com.example.core.mvi.KeyedEffect
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.OrderRequest

/**
 * Side effects a reducer asks for, as plain data so tests can assert on them. A [KeyedEffect] is
 * latest-wins: starting one cancels the running one with the same key.
 */
internal sealed interface CheckoutEffect {

    data object ObserveContext : CheckoutEffect, KeyedEffect {
        override val cancelKey: Any get() = "context"
    }

    data object RefreshCart : CheckoutEffect
    data object ResetDemo : CheckoutEffect

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
