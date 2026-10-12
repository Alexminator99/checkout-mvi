package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.Address
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.repository.AddressRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class AddressRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : AddressRepository {

    override fun observeAddresses(): Flow<CheckoutOutcome<List<Address>>> = flow {
        emit(safeCall { remote.addresses().map { it.toDomain() } })
    }

    override suspend fun validate(addressId: String): CheckoutOutcome<AddressValidation> =
        safeCall { remote.validateAddress(addressId).toDomain() }
}
