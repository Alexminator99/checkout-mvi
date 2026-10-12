package com.example.feature.checkout.data.remote.dto

internal data class QuoteRequestDto(val addressId: String, val lines: List<QuoteLineDto>)
