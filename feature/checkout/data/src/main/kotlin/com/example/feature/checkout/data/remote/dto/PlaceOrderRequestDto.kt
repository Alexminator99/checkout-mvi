package com.example.feature.checkout.data.remote.dto

internal data class PlaceOrderRequestDto(
    val addressId: String,
    val shippingOptionId: String,
    val paymentMethodId: String,
    val promoCode: String?,
)
