package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.model.AddressValidation

internal data class AddressSlice(
    val selectedId: String? = null,
    val validation: Loadable<AddressValidation> = Loadable.Idle,
)
