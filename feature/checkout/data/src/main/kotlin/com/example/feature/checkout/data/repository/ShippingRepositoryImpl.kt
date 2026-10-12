package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.mapper.toQuoteLines
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.data.remote.dto.QuoteRequestDto
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.repository.ShippingRepository
import javax.inject.Inject

internal class ShippingRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : ShippingRepository {

    override suspend fun quote(addressId: String, cart: Cart): CheckoutOutcome<List<ShippingOption>> = safeCall {
        remote.shippingQuotes(QuoteRequestDto(addressId, cart.toQuoteLines())).map { it.toDomain() }
    }
}
