package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.OFFICE
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.repository.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeAddressRepository(
    var addresses: CheckoutOutcome<List<Address>> = Outcome.Success(listOf(HOME, OFFICE)),
    var validation: CheckoutOutcome<AddressValidation> = Outcome.Success(AddressValidation.Deliverable),
) : AddressRepository {

    val validatedIds = mutableListOf<String>()

    // Like a remote fetch: each subscription reads the configured result once.
    override fun observeAddresses(): Flow<CheckoutOutcome<List<Address>>> = flow { emit(addresses) }

    override suspend fun validate(addressId: String): CheckoutOutcome<AddressValidation> {
        validatedIds += addressId
        return validation
    }
}
