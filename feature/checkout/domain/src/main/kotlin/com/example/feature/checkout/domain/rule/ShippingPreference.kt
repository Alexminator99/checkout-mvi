package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.ShippingOption

/**
 * Keeps the current choice while it is still offered. Otherwise picks the option that costs this
 * customer the least, preferring the faster one when prices tie.
 */
fun List<ShippingOption>.preferredFor(currentId: String?, customer: Customer): ShippingOption? =
    firstOrNull { it.id == currentId }
        ?: minWithOrNull(compareBy<ShippingOption> { it.priceFor(customer) }.thenBy { it.estimatedDays })
