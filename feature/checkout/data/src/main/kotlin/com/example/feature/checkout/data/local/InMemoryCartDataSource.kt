package com.example.feature.checkout.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The cart kept in memory, seeded with two demo lines. It starts over when the process restarts. */
internal class InMemoryCartDataSource(initialLines: List<CartLineRecord> = DEMO_LINES) : CartDataSource {

    private val lines = MutableStateFlow(initialLines)

    override fun observeLines(): Flow<List<CartLineRecord>> = lines.asStateFlow()

    override suspend fun updateQuantity(sku: String, quantity: Int): Boolean = edit(sku) { current ->
        current.map { if (it.sku == sku) it.copy(quantity = quantity) else it }
    }

    override suspend fun delete(sku: String): Boolean = edit(sku) { current ->
        current.filterNot { it.sku == sku }
    }

    private fun edit(sku: String, change: (List<CartLineRecord>) -> List<CartLineRecord>): Boolean {
        if (lines.value.none { it.sku == sku }) return false
        lines.update(change)
        return true
    }

    companion object {
        val DEMO_LINES: List<CartLineRecord> = listOf(
            CartLineRecord(sku = "mug", name = "Coffee mug", unitPriceMinor = 1200, currency = "EUR", quantity = 2),
            CartLineRecord(sku = "tee", name = "T-shirt", unitPriceMinor = 2000, currency = "EUR", quantity = 1),
        )
    }
}
