package com.example.feature.checkout.data.local

internal data class CartLineRecord(
    val sku: String,
    val name: String,
    val unitPriceMinor: Long,
    val currency: String,
    val quantity: Int,
)
