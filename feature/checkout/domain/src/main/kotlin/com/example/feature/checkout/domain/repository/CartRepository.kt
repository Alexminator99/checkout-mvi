package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import kotlinx.coroutines.flow.Flow

interface CartRepository {
    fun observeCart(): Flow<Cart>
    /** Returns the committed cart, independently of when observers receive it. */
    suspend fun setQuantity(sku: String, quantity: Int): CheckoutOutcome<Cart>
    suspend fun removeLine(sku: String): CheckoutOutcome<Cart>
    suspend fun refresh(): CheckoutOutcome<Cart>
    suspend fun restoreDemoCart(): CheckoutOutcome<Cart>
}
