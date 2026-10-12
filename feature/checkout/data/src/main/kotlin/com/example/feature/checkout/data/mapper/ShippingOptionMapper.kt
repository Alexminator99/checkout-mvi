package com.example.feature.checkout.data.mapper

import com.example.core.domain.Money
import com.example.feature.checkout.data.remote.dto.ShippingOptionDto
import com.example.feature.checkout.domain.model.ShippingOption

internal fun ShippingOptionDto.toDomain(): ShippingOption =
    ShippingOption(id, label, Money.ofMinor(priceMinor, currency), etaDays, express)
