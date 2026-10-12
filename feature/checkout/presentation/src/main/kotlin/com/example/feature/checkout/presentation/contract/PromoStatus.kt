package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.Promo

internal sealed interface PromoStatus {
    data object None : PromoStatus
    data class Redeeming(val code: String) : PromoStatus
    data class Applied(val promo: Promo) : PromoStatus
    data class Rejected(val error: CheckoutError) : PromoStatus
}
