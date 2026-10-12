package com.example.feature.checkout.domain.model

/** Everything the checkout observes from the outside world, as one read model. */
data class CheckoutContext(
    val cart: Cart,
    val addresses: List<Address>,
    val paymentMethods: List<PaymentMethod>,
    val customer: Customer,
    val isOnline: Boolean,
    val isExpressShippingEnabled: Boolean,
)
