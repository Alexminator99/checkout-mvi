package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.repository.ShippingRepository

class FakeShippingRepository(
    var quotes: CheckoutOutcome<List<ShippingOption>> = Outcome.Success(listOf(STANDARD, PICKUP, EXPRESS)),
) : ShippingRepository {

    val quotedAddressIds = mutableListOf<String>()

    override suspend fun quote(addressId: String, cart: Cart): CheckoutOutcome<List<ShippingOption>> {
        quotedAddressIds += addressId
        return quotes
    }
}
