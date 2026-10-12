package com.example.feature.checkout.data.mapper

import com.example.core.domain.Money
import com.example.feature.checkout.data.local.CartLineRecord
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.CartLine
import java.util.Currency

internal fun List<CartLineRecord>.toCart(fallbackCurrency: Currency): Cart = Cart(
    lines = map { CartLine(it.sku, it.name, Money.ofMinor(it.unitPriceMinor, it.currency), it.quantity) },
    currency = firstOrNull()?.let { Currency.getInstance(it.currency) } ?: fallbackCurrency,
)
