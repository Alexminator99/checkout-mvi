package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.Order

sealed interface OrderAction : CheckoutAction {
    data object PlaceClicked : OrderAction, CheckoutIntent
    data object ErrorDismissed : OrderAction, CheckoutIntent
    data class Placed(val order: Order) : OrderAction
    data class Failed(val error: CheckoutError) : OrderAction
}
