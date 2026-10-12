package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.AddressValidation
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    /** Emits an outcome, so a failed fetch is a value rather than an exception that ends the stream. */
    fun observeAddresses(): Flow<CheckoutOutcome<List<Address>>>
    suspend fun validate(addressId: String): CheckoutOutcome<AddressValidation>
}
