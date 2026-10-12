package com.example.feature.checkout.domain.usecase

import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.repository.AddressRepository
import javax.inject.Inject

class ValidateAddressUseCase @Inject constructor(private val addressRepository: AddressRepository) {

    suspend operator fun invoke(addressId: String): CheckoutOutcome<AddressValidation> =
        addressRepository.validate(addressId)
}
