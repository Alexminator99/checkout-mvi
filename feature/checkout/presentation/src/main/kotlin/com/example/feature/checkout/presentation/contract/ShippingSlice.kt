package com.example.feature.checkout.presentation.contract

import com.example.feature.checkout.domain.model.ShippingOption

internal data class ShippingSlice(
    val quotes: Loadable<List<ShippingOption>> = Loadable.Idle,
    val selectedId: String? = null,
)
