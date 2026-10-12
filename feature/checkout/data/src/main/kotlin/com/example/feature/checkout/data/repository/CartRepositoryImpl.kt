package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.di.StoreCurrency
import com.example.feature.checkout.data.local.CartDataSource
import com.example.feature.checkout.data.mapper.toCart
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.repository.CartRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Currency
import javax.inject.Inject

internal class CartRepositoryImpl @Inject constructor(
    private val cartDataSource: CartDataSource,
    @StoreCurrency private val storeCurrency: Currency,
) : CartRepository {

    override fun observeCart(): Flow<Cart> = cartDataSource.observeLines().map { it.toCart(storeCurrency) }

    override suspend fun setQuantity(sku: String, quantity: Int): CheckoutOutcome<Unit> =
        written(cartDataSource.updateQuantity(sku, quantity))

    override suspend fun removeLine(sku: String): CheckoutOutcome<Unit> = written(cartDataSource.delete(sku))

    private fun written(found: Boolean): CheckoutOutcome<Unit> =
        if (found) Outcome.Success(Unit) else Outcome.Failure(CheckoutError.NotFound)
}
