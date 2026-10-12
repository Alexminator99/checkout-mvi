package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.PaymentMethodDto
import com.example.feature.checkout.domain.model.PaymentMethod

internal fun PaymentMethodDto.toDomain(): PaymentMethod = PaymentMethod(id, brand, last4, isDefault)
