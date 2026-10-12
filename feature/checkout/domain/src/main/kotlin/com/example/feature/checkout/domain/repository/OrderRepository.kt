package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest

interface OrderRepository {
    suspend fun place(request: OrderRequest): CheckoutOutcome<Order>
}
