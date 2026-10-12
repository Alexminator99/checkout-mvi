package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.CheckoutState
import com.example.feature.checkout.presentation.contract.isPlacingOrder
import com.example.feature.checkout.presentation.contract.orderRequestOrNull
import com.example.feature.checkout.presentation.ui.model.PlaceOrderButtonUi
import javax.inject.Inject

internal class PlaceOrderButtonProjector @Inject constructor() {

    fun project(state: CheckoutState, total: String): PlaceOrderButtonUi = PlaceOrderButtonUi(
        label = UiText.res(R.string.checkout_pay_amount, total),
        enabled = state.orderRequestOrNull() != null, // the same rule the order reducer guards with
        loading = state.isPlacingOrder,
    )
}
