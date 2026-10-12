package com.example.feature.checkout.domain.usecase

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fake.FakePromoRepository
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.cartOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RedeemPromoUseCaseTest {

    @Test
    fun `codes are trimmed and upper-cased before redeeming`() = runTest {
        val repository = FakePromoRepository()

        RedeemPromoUseCase(repository)("  save5 ", cartOf(MUG))

        assertEquals(listOf("SAVE5"), repository.redeemedCodes)
    }

    @Test
    fun `a rejection is reported`() = runTest {
        val repository = FakePromoRepository(result = Outcome.Failure(CheckoutError.Promo.Expired))

        assertEquals(Outcome.Failure(CheckoutError.Promo.Expired), RedeemPromoUseCase(repository)("OLD", cartOf(MUG)))
    }
}
