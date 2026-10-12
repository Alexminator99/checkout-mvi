package com.example.feature.checkout.presentation.contract

internal data class PromoSlice(
    val input: String = "",
    val status: PromoStatus = PromoStatus.None,
)
