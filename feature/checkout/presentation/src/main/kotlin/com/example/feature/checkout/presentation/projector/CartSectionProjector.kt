package com.example.feature.checkout.presentation.projector

import com.example.core.ui.MoneyFormatter
import com.example.core.ui.UiText
import com.example.feature.checkout.domain.rule.MAX_QUANTITY_PER_LINE
import com.example.feature.checkout.presentation.R
import com.example.feature.checkout.presentation.contract.isCheckoutLocked
import com.example.feature.checkout.presentation.ui.model.CartLineUi
import com.example.feature.checkout.presentation.ui.model.CartSectionUi
import javax.inject.Inject

internal class CartSectionProjector @Inject constructor(private val money: MoneyFormatter) {

    fun project(ready: ReadyCheckout): CartSectionUi {
        val cart = ready.context.cart
        val editable = !ready.state.isCheckoutLocked && !ready.state.isUpdatingCart
        return CartSectionUi(
            title = UiText.plural(R.plurals.checkout_item_count, cart.itemCount, cart.itemCount),
            lines = cart.lines.map { line ->
                CartLineUi(
                    sku = line.sku,
                    name = line.name,
                    quantity = line.quantity,
                    lineTotal = money.format(line.lineTotal),
                    canDecrement = editable,
                    decrementRemovesLine = line.quantity == 1,
                    canIncrement = editable && line.quantity < MAX_QUANTITY_PER_LINE,
                )
            },
            isEmpty = cart.isEmpty,
        )
    }
}
