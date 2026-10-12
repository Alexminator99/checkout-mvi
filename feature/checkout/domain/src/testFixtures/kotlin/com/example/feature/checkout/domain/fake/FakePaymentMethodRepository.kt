package com.example.feature.checkout.domain.fake

import com.example.core.domain.Outcome
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.fixture.MASTERCARD
import com.example.feature.checkout.domain.fixture.VISA
import com.example.feature.checkout.domain.model.PaymentMethod
import com.example.feature.checkout.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakePaymentMethodRepository(
    var paymentMethods: CheckoutOutcome<List<PaymentMethod>> = Outcome.Success(listOf(VISA, MASTERCARD)),
) : PaymentMethodRepository {

    override fun observePaymentMethods(): Flow<CheckoutOutcome<List<PaymentMethod>>> = flow { emit(paymentMethods) }
}
