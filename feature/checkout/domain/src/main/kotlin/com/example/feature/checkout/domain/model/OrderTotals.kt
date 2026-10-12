package com.example.feature.checkout.domain.model

import com.example.core.domain.Money

data class OrderTotals(
    val subtotal: Money,
    val shipping: Money,
    val discount: Money,
    val total: Money,
)
