package com.example.feature.checkout.domain.model

data class PaymentMethod(
    val id: String,
    val brand: String,
    val last4: String,
    val isDefault: Boolean,
)
