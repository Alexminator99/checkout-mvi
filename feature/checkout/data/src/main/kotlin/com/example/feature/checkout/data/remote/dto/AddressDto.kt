package com.example.feature.checkout.data.remote.dto

internal data class AddressDto(
    val id: String,
    val label: String,
    val recipient: String,
    val line1: String,
    val city: String,
    val postalCode: String,
    val isDefault: Boolean,
)
