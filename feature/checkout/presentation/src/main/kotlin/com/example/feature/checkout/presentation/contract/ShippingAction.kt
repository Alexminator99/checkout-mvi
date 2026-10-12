package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.ShippingOption

sealed interface ShippingAction : CheckoutAction {
    data class Selected(val optionId: String) : ShippingAction, CheckoutIntent
    data object RetryClicked : ShippingAction, CheckoutIntent
    data class QuotesLoaded(val addressId: String, val options: List<ShippingOption>) : ShippingAction
    data class QuotesFailed(val addressId: String, val error: CheckoutError) : ShippingAction
}
