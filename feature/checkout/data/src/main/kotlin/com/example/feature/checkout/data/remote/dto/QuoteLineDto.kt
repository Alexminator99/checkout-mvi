package com.example.feature.checkout.data.remote.dto

internal data class QuoteLineDto(
    val sku: String,
    val quantity: Int,
    val unitPriceMinor: Long,
    val currency: String,
)
