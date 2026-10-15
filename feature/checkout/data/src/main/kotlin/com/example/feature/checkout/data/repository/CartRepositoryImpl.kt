package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.di.StoreCurrency
import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.local.CartDataSource
import com.example.feature.checkout.data.local.InMemoryCartDataSource
import com.example.feature.checkout.data.mapper.toCart
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.data.remote.dto.QuoteLineDto
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.repository.CartRepository
import java.util.Currency
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class CartRepositoryImpl @Inject constructor(
    private val cartDataSource: CartDataSource,
    @StoreCurrency private val storeCurrency: Currency,
    private val remote: CheckoutRemoteDataSource,
) : CartRepository {

    override fun observeCart(): Flow<Cart> = cartDataSource.observeLines().map { it.toCart(storeCurrency) }

    override suspend fun setQuantity(sku: String, quantity: Int): CheckoutOutcome<Cart> =
        written(cartDataSource.updateQuantity(sku, quantity))

    override suspend fun removeLine(sku: String): CheckoutOutcome<Cart> = written(cartDataSource.delete(sku))

    override suspend fun refresh(): CheckoutOutcome<Cart> = safeCall {
        val current = cartDataSource.observeLines().first()
        val refreshed = remote.refreshCart(current.map {
            QuoteLineDto(it.sku, it.quantity, it.unitPriceMinor, it.currency)
        }).associateBy { it.sku }
        val updated = current.map { line ->
            val price = requireNotNull(refreshed[line.sku])
            require(price.unitPriceMinor >= 0 && price.currency == line.currency)
            line.copy(unitPriceMinor = price.unitPriceMinor, currency = price.currency)
        }
        val cart = updated.toCart(storeCurrency)
        cartDataSource.replaceLines(updated)
        cart
    }

    override suspend fun restoreDemoCart(): CheckoutOutcome<Cart> = safeCall {
        cartDataSource.replaceLines(InMemoryCartDataSource.DEMO_LINES)
        observeCart().first()
    }

    private suspend fun written(found: Boolean): CheckoutOutcome<Cart> =
        if (found) Outcome.Success(observeCart().first()) else Outcome.Failure(CheckoutError.NotFound)
}
