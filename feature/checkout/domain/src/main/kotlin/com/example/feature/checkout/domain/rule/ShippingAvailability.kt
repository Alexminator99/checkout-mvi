package com.example.feature.checkout.domain.rule

import com.example.feature.checkout.domain.model.ShippingOption

/** Express options are offered only while the express-shipping flag is on. */
fun List<ShippingOption>.availableFor(isExpressEnabled: Boolean): List<ShippingOption> =
    filter { isExpressEnabled || !it.isExpress }
