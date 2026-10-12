package com.example.feature.checkout.domain.model

import com.example.core.domain.Money

data class ShippingOption(
    val id: String,
    val label: String,
    val price: Money,
    val estimatedDays: Int,
    val isExpress: Boolean,
)
