package com.example.feature.checkout.domain.model

/**
 * One attempt to place an order.
 *
 * [checkoutId] identifies the checkout the request comes from. Retries within one checkout send the
 * same request, so the backend can recognise them, while an identical order placed in a later
 * checkout is a new order.
 */
data class OrderRequest(
    val checkoutId: String,
    val addressId: String,
    val shippingOptionId: String,
    val paymentMethodId: String,
    val promoCode: String?,
)
