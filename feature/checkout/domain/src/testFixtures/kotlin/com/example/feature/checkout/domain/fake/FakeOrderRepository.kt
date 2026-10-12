package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.domain.repository.OrderRepository

class FakeOrderRepository(var result: CheckoutOutcome<Order> = Outcome.Success(Order("order-1"))) : OrderRepository {

    val placedRequests = mutableListOf<OrderRequest>()

    override suspend fun place(request: OrderRequest): CheckoutOutcome<Order> {
        placedRequests += request
        return result
    }
}
