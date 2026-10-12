package com.example.feature.checkout.presentation.projector

import com.example.core.domain.Money
import com.example.core.ui.MoneyFormatter
import com.example.core.ui.UiText
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.usecase.CalculateOrderTotalsUseCase
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.appliedPromo
import com.example.feature.checkout.presentation.contract.selectedShippingOption
import com.example.feature.checkout.presentation.ui.model.SummaryRowUi
import com.example.feature.checkout.presentation.ui.model.SummaryUi
import javax.inject.Inject

/** Formats the totals. Computing them is a domain rule, so it is left to [CalculateOrderTotalsUseCase]. */
internal class SummarySectionProjector @Inject constructor(
    private val money: MoneyFormatter,
    private val calculateOrderTotals: CalculateOrderTotalsUseCase,
) {

    fun project(ready: ReadyCheckout): SummaryUi {
        val shippingOption = ready.state.selectedShippingOption
        val totals = calculateOrderTotals(ready.context.cart, shippingOption, ready.state.appliedPromo, ready.context.customer)
        val rows = buildList<SummaryRowUi> {
            add(SummaryRowUi(UiText.res(R.string.checkout_subtotal), UiText.Raw(money.format(totals.subtotal))))
            add(SummaryRowUi(UiText.res(R.string.checkout_shipping), shippingText(shippingOption, totals.shipping)))
            if (!totals.discount.isZero) {
                add(
                    SummaryRowUi(
                        UiText.res(R.string.checkout_discount),
                        UiText.res(R.string.checkout_discount_amount, money.format(totals.discount)),
                    ),
                )
            }
        }
        return SummaryUi(rows = rows, total = money.format(totals.total))
    }

    private fun shippingText(option: ShippingOption?, cost: Money): UiText = when {
        option == null -> UiText.res(R.string.checkout_shipping_pending)
        cost.isZero -> UiText.res(R.string.checkout_free)
        else -> UiText.Raw(money.format(cost))
    }
}
