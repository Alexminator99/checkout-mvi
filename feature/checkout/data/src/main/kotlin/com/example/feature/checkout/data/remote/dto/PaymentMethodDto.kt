package com.example.feature.checkout.data.remote.dto

internal data class PaymentMethodDto(
    val id: String,
    val brand: String,
    val last4: String,
    val isDefault: Boolean,
)
