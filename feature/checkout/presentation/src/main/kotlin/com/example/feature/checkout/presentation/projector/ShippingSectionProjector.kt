package com.example.feature.checkout.presentation.projector

import com.example.core.ui.MoneyFormatter
import com.example.core.ui.UiText
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.rule.priceFor
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.Loadable
import com.example.feature.checkout.presentation.contract.availableShippingOptions
import com.example.feature.checkout.presentation.contract.isCheckoutLocked
import com.example.feature.checkout.presentation.ui.model.SelectableRowUi
import com.example.feature.checkout.presentation.ui.model.ShippingSectionUi
import javax.inject.Inject

internal class ShippingSectionProjector @Inject constructor(private val money: MoneyFormatter) {

    fun project(ready: ReadyCheckout): ShippingSectionUi = when (val quotes = ready.state.shipping.quotes) {
        Loadable.Idle -> ShippingSectionUi.Hidden
        Loadable.Loading -> ShippingSectionUi.Loading
        is Loadable.Failed -> ShippingSectionUi.Error(quotes.error.toUiText())
        is Loadable.Ready -> ShippingSectionUi.Options(
            ready.state.availableShippingOptions.map { option ->
                SelectableRowUi(
                    id = option.id,
                    title = UiText.Raw(option.label),
                    subtitle = UiText.plural(R.plurals.checkout_delivery_days, option.estimatedDays, option.estimatedDays),
                    trailing = priceText(option, ready.context.customer),
                    selected = option.id == ready.state.shipping.selectedId,
                    enabled = !ready.state.isCheckoutLocked,
                )
            },
        )
    }

    private fun priceText(option: ShippingOption, customer: Customer): UiText {
        val price = option.priceFor(customer)
        return when {
            price.isZero && customer.isMember -> UiText.res(R.string.checkout_free_for_members)
            price.isZero -> UiText.res(R.string.checkout_free)
            else -> UiText.Raw(money.format(price))
        }
    }
}
