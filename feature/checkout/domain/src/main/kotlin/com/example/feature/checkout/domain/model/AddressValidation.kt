package com.example.feature.checkout.domain.model

sealed interface AddressValidation {
    data object Deliverable : AddressValidation
    data class Undeliverable(val reason: Reason) : AddressValidation

    enum class Reason { OUTSIDE_SERVICE_AREA, INCOMPLETE, PO_BOX_NOT_ALLOWED }
}
