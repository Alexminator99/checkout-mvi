package com.example.feature.checkout.presentation.projector

import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.isPlacingOrder
import com.example.feature.checkout.presentation.ui.model.PaymentSectionUi
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import javax.inject.Inject

internal class PaymentSectionProjector @Inject constructor() {

    fun project(ready: ReadyCheckout): PaymentSectionUi = PaymentSectionUi(
        rows = ready.context.paymentMethods.map { method ->
            SelectableRowUi(
                id = method.id,
                title = UiText.res(R.string.checkout_card_label, method.brand, method.last4),
                subtitle = null,
                trailing = null,
                selected = method.id == ready.state.selectedPaymentId,
                enabled = !ready.state.isPlacingOrder,
            )
        },
    )
}
