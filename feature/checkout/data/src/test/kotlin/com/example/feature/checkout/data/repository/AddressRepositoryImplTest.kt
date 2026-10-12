package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.model.AddressValidation
import com.example.feature.checkout.domain.model.AddressValidation.Reason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AddressRepositoryImplTest {

    private val repository = AddressRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))

    @Test
    fun `lists the demo addresses with Home as the default`() = runTest {
        val addresses = (repository.observeAddresses().first() as Outcome.Success).value

        assertEquals(listOf("Home", "Office", "PO Box", "Island cabin"), addresses.map { it.label })
        assertEquals(listOf(DemoCheckoutData.HOME_ID), addresses.filter { it.isDefault }.map { it.id })
    }

    @Test
    fun `home and office are deliverable`() = runTest {
        assertEquals(Outcome.Success(AddressValidation.Deliverable), repository.validate(DemoCheckoutData.HOME_ID))
        assertEquals(Outcome.Success(AddressValidation.Deliverable), repository.validate(DemoCheckoutData.OFFICE_ID))
    }

    @Test
    fun `the PO box is refused`() = runTest {
        assertEquals(
            Outcome.Success(AddressValidation.Undeliverable(Reason.PO_BOX_NOT_ALLOWED)),
            repository.validate(DemoCheckoutData.PO_BOX_ID),
        )
    }

    @Test
    fun `the island cabin is outside the service area`() = runTest {
        assertEquals(
            Outcome.Success(AddressValidation.Undeliverable(Reason.OUTSIDE_SERVICE_AREA)),
            repository.validate(DemoCheckoutData.ISLAND_ID),
        )
    }

    @Test
    fun `an unknown address is not found`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.NotFound), repository.validate("nowhere"))
    }
}
