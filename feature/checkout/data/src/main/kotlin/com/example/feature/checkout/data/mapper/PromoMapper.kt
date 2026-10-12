package com.example.feature.checkout.data.mapper

import com.example.core.domain.Money
import com.example.feature.checkout.data.remote.dto.PromoDto
import com.example.feature.checkout.domain.model.Promo

internal fun PromoDto.toDomain(): Promo = Promo(code, Money.ofMinor(discountMinor, currency))
