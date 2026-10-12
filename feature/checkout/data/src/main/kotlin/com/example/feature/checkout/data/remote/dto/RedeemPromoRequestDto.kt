package com.example.feature.checkout.data.remote.dto

internal data class RedeemPromoRequestDto(val code: String, val lines: List<QuoteLineDto>)
