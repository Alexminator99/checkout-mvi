package com.example.feature.checkout.data.repository

import com.example.core.domain.Outcome
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.model.PaymentMethod
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentMethodRepositoryImplTest {

    @Test
    fun `lists the three demo cards with Visa as the default`() = runTest {
        val repository = PaymentMethodRepositoryImpl(DemoCheckoutRemoteDataSource(latencyMillis = 0))

        assertEquals(
            Outcome.Success(listOf(VISA, MASTERCARD, PaymentMethod("amex", "Amex", "3155", isDefault = false))),
            repository.observePaymentMethods().first(),
        )
    }
}
