package com.example.feature.checkout.data.mapper

import com.example.feature.checkout.data.remote.dto.OrderDto
import com.example.feature.checkout.data.remote.dto.PlaceOrderRequestDto
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest

internal fun OrderRequest.toDto(): PlaceOrderRequestDto =
    PlaceOrderRequestDto(addressId, shippingOptionId, paymentMethodId, promoCode)

internal fun OrderDto.toDomain(): Order = Order(id)
