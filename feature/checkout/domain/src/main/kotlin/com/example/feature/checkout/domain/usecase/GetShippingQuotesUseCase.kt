package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Cart
import com.example.feature.checkout.domain.model.ShippingOption
import com.example.feature.checkout.domain.repository.ShippingRepository
import javax.inject.Inject

class GetShippingQuotesUseCase @Inject constructor(private val shippingRepository: ShippingRepository) {

    suspend operator fun invoke(addressId: String, cart: Cart): CheckoutOutcome<List<ShippingOption>> =
        shippingRepository.quote(addressId, cart)
}
