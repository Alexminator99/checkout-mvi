package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.fixture.EXPRESS
import com.example.feature.checkout.domain.fixture.MUG
import com.example.feature.checkout.domain.fixture.PICKUP
import com.example.feature.checkout.domain.fixture.STANDARD
import com.example.feature.checkout.domain.fixture.cartOf
import com.example.feature.checkout.domain.model.ShippingOption
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ShippingRepositoryImplTest {

    private val repository = ShippingRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))

    @Test
    fun `a cart with items gets three options`() = runTest {
        assertEquals(
            Outcome.Success(listOf(STANDARD, PICKUP, EXPRESS)),
            repository.quote(DemoCheckoutData.HOME_ID, cartOf(MUG)),
        )
    }

    @Test
    fun `an empty cart gets no options`() = runTest {
        assertEquals(Outcome.Success(emptyList<ShippingOption>()), repository.quote(DemoCheckoutData.HOME_ID, cartOf()))
    }
}
