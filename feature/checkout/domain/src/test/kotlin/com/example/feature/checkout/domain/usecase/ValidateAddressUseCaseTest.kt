package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.fake.FakeAddressRepository
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.model.AddressValidation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ValidateAddressUseCaseTest {

    @Test
    fun `asks the repository about the given address`() = runTest {
        val undeliverable = AddressValidation.Undeliverable(AddressValidation.Reason.PO_BOX_NOT_ALLOWED)
        val repository = FakeAddressRepository(validation = Outcome.Success(undeliverable))

        val result = ValidateAddressUseCase(repository)(HOME.id)

        assertEquals(listOf(HOME.id), repository.validatedIds)
        assertEquals(Outcome.Success(undeliverable), result)
    }
}
