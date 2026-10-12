package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutError
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.fixture.eur
import com.example.feature.checkout.domain.model.Promo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PromoRepositoryImplTest {

    private val repository = PromoRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))
    private val smallCart = cartOf(MUG) // 24.00
    private val bigCart = cartOf(MUG.copy(quantity = 10)) // 120.00

    @Test
    fun `SAVE5 takes five euros off`() = runTest {
        assertEquals(Outcome.Success(Promo("SAVE5", eur("5.00"))), repository.redeem("SAVE5", smallCart))
    }

    @Test
    fun `BIG50 needs an order of at least 100 euros`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Promo.MinimumNotReached), repository.redeem("BIG50", smallCart))
        assertEquals(Outcome.Success(Promo("BIG50", eur("50.00"))), repository.redeem("BIG50", bigCart))
    }

    @Test
    fun `EXPIRED has expired`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Promo.Expired), repository.redeem("EXPIRED", smallCart))
    }

    @Test
    fun `unknown codes are invalid`() = runTest {
        assertEquals(Outcome.Failure(CheckoutError.Promo.Invalid), repository.redeem("NOPE", smallCart))
    }
}
