package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.mapper.toDto
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.data.remote.HttpStatus
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Order
import com.example.feature.checkout.domain.model.OrderRequest
import com.example.feature.checkout.domain.repository.OrderRepository
import java.util.UUID
import javax.inject.Inject

internal class OrderRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : OrderRepository {

    override suspend fun place(request: OrderRequest): CheckoutOutcome<Order> = safeCall(
        mapApiError = { error ->
            when (error.code) {
                HttpStatus.PAYMENT_REQUIRED -> CheckoutError.Payment.Declined
                HttpStatus.FORBIDDEN -> CheckoutError.Payment.RequiresAction
                HttpStatus.UNPROCESSABLE_ENTITY -> CheckoutError.OutOfStock
                else -> null
            }
        },
    ) {
        remote.placeOrder(request.idempotencyKey(), request.toDto()).toDomain()
    }

    /**
     * The same request within one checkout always yields the same key, so a retry after a timeout
     * cannot charge twice. A new checkout has a new checkout id, so a later identical order is a
     * new order.
     */
    private fun OrderRequest.idempotencyKey(): String {
        val fields = listOf(checkoutId, addressId, shippingOptionId, paymentMethodId, promoCode.orEmpty())
        return UUID.nameUUIDFromBytes(fields.joinToString("|").toByteArray()).toString()
    }
}
