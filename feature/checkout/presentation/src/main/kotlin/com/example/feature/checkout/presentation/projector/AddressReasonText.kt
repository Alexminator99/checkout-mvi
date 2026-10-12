package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.presentation.R

internal fun AddressValidation.Reason.toUiText(): UiText = UiText.res(
    when (this) {
        AddressValidation.Reason.OUTSIDE_SERVICE_AREA -> R.string.address_outside_area
        AddressValidation.Reason.INCOMPLETE -> R.string.address_incomplete
        AddressValidation.Reason.PO_BOX_NOT_ALLOWED -> R.string.address_po_box
    },
)
