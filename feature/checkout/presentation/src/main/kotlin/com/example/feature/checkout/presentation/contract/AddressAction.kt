package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.AddressValidation

sealed interface AddressAction : CheckoutAction {
    data class Selected(val addressId: String) : AddressAction, CheckoutIntent
    data class Validated(val addressId: String, val result: AddressValidation) : AddressAction
    data class ValidationFailed(val addressId: String, val error: CheckoutError) : AddressAction
}
