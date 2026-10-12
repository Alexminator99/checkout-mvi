package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.AddressDto
import com.example.feature.checkout.domain.model.Address

internal fun AddressDto.toDomain(): Address = Address(id, label, recipient, line1, city, postalCode, isDefault)
