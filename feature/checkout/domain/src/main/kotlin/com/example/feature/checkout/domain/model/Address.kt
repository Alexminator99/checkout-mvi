package com.example.feature.checkout.domain.model

data class Address(
    val id: String,
    val label: String,
    val recipient: String,
    val line1: String,
    val city: String,
    val postalCode: String,
    val isDefault: Boolean,
)
