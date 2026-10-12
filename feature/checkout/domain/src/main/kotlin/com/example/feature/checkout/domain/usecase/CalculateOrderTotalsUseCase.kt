package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Money
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.OrderTotals
import com.example.feature.checkout.domain.model.Promo
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.rule.priceFor
import javax.inject.Inject

/**
 * The pricing of an order, shared by every place that shows a total. Deciding the total belongs
 * here; formatting it belongs to presentation.
 */
class CalculateOrderTotalsUseCase @Inject constructor() {

    operator fun invoke(cart: Cart, shipping: ShippingOption?, promo: Promo?, customer: Customer): OrderTotals {
        val zero = Money.zero(cart.currency)
        val subtotal = cart.subtotal
        val shippingCost = shipping?.priceFor(customer) ?: zero
        val discount = promo?.discount?.coerceAtMost(subtotal) ?: zero
        return OrderTotals(subtotal, shippingCost, discount, subtotal + shippingCost - discount)
    }
}
