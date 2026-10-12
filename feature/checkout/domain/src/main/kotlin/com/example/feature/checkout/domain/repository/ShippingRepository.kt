package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.ShippingOption

interface ShippingRepository {
    suspend fun quote(addressId: String, cart: Cart): CheckoutOutcome<List<ShippingOption>>
}
