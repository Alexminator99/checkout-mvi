package com.example.feature.checkout.presentation.projector

import com.example.core.ui.MoneyFormatter
import com.example.core.ui.UiText
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.PromoStatus
import com.example.feature.checkout.presentation.contract.appliedPromo
import com.example.feature.checkout.presentation.contract.isPlacingOrder
import com.example.feature.checkout.presentation.ui.model.PromoSectionUi
import javax.inject.Inject

internal class PromoSectionProjector @Inject constructor(private val money: MoneyFormatter) {

    fun project(ready: ReadyCheckout): PromoSectionUi {
        val promo = ready.state.promo
        val editable = !ready.state.isPlacingOrder &&
            (promo.status is PromoStatus.None || promo.status is PromoStatus.Rejected)
        return PromoSectionUi(
            input = promo.input,
            inputEnabled = editable,
            applyEnabled = editable && promo.input.isNotBlank() && !ready.context.cart.isEmpty,
            removeEnabled = !ready.state.isPlacingOrder,
            isRedeeming = promo.status is PromoStatus.Redeeming,
            applied = ready.state.appliedPromo?.let { UiText.res(R.string.checkout_promo_applied, it.code, money.format(it.discount)) },
            error = (promo.status as? PromoStatus.Rejected)?.error?.toUiText(),
        )
    }
}
