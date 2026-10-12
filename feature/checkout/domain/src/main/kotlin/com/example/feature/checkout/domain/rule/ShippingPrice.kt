package com.example.feature.checkout.domain.rule

import com.example.core.domain.Money
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.model.ShippingOption

/** Members ship for free unless they choose express delivery. The only place this perk is defined. */
fun ShippingOption.priceFor(customer: Customer): Money =
    if (customer.isMember && !isExpress) Money.zero(price.currency) else price
