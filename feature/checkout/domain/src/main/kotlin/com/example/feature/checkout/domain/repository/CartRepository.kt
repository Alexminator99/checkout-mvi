package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<Cart>
    suspend fun setQuantity(sku: String, quantity: Int): CheckoutOutcome<Unit>
    suspend fun removeLine(sku: String): CheckoutOutcome<Unit>
}
