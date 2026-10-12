package com.example.feature.checkout.domain.model

import com.example.core.domain.Money

data class CartLine(
    val sku: String,
    val name: String,
    val unitPrice: Money,
    val quantity: Int,
) {
    val lineTotal: Money get() = unitPrice * quantity
}
