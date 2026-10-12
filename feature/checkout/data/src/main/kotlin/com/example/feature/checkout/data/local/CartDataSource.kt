package com.example.feature.checkout.data.local

import kotlinx.coroutines.flow.Flow

/** Where the cart is kept on the device. In a production app, Room would implement this. */
internal interface CartDataSource {
    fun observeLines(): Flow<List<CartLineRecord>>

    /** @return false when no line has [sku]. */
    suspend fun updateQuantity(sku: String, quantity: Int): Boolean

    /** @return false when no line has [sku]. */
    suspend fun delete(sku: String): Boolean
}
