package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.CheckoutContext

sealed interface ContextAction : CheckoutAction {
    data class Updated(val context: CheckoutContext) : ContextAction
    data class Failed(val error: CheckoutError) : ContextAction
}
