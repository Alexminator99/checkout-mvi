package com.example.feature.checkout.domain.model

import com.example.core.domain.Money
import java.util.Currency

data class Cart(val lines: List<CartLine>, val currency: Currency) {
    val isEmpty: Boolean get() = lines.isEmpty()
    val itemCount: Int get() = lines.sumOf { it.quantity }
    val subtotal: Money get() = lines.fold(Money.zero(currency)) { total, line -> total + line.lineTotal }
}
