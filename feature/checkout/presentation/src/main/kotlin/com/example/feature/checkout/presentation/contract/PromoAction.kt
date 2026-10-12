package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.Promo

sealed interface PromoAction : CheckoutAction {
    data class InputChanged(val text: String) : PromoAction, CheckoutIntent
    data object ApplyClicked : PromoAction, CheckoutIntent
    data object RemoveClicked : PromoAction, CheckoutIntent
    data class Redeemed(val promo: Promo) : PromoAction
    data class Rejected(val error: CheckoutError) : PromoAction
}
