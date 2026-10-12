package com.example.feature.checkout.data.remote.dto

internal data class ShippingOptionDto(
    val id: String,
    val label: String,
    val priceMinor: Long,
    val currency: String,
    val etaDays: Int,
    val express: Boolean,
)
