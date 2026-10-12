package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.fake.FakeShippingRepository
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.HOME
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.cartOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetShippingQuotesUseCaseTest {

    @Test
    fun `quotes the given address`() = runTest {
        val repository = FakeShippingRepository()

        val result = GetShippingQuotesUseCase(repository)(HOME.id, cartOf(MUG))

        assertEquals(listOf(HOME.id), repository.quotedAddressIds)
        assertEquals(Outcome.Success(listOf(STANDARD, PICKUP, EXPRESS)), result)
    }
}
