package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.AddressValidationDto
import com.example.feature.checkout.domain.model.AddressValidation

internal fun AddressValidationDto.toDomain(): AddressValidation =
    if (deliverable) AddressValidation.Deliverable
    else AddressValidation.Undeliverable(reasonCode.toReason())

private fun String?.toReason(): AddressValidation.Reason = when (this) {
    "PO_BOX" -> AddressValidation.Reason.PO_BOX_NOT_ALLOWED
    "INCOMPLETE" -> AddressValidation.Reason.INCOMPLETE
    else -> AddressValidation.Reason.OUTSIDE_SERVICE_AREA
}
