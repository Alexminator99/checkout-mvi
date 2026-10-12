package com.example.feature.checkout.data.repository

import com.example.feature.checkout.data.error.safeCall
import com.example.feature.checkout.data.mapper.toDomain
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.domain.error.CheckoutOutcome
import com.example.feature.checkout.domain.model.PaymentMethod
import com.example.feature.checkout.domain.repository.PaymentMethodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

internal class PaymentMethodRepositoryImpl @Inject constructor(
    private val remote: CheckoutRemoteDataSource,
) : PaymentMethodRepository {

    override fun observePaymentMethods(): Flow<CheckoutOutcome<List<PaymentMethod>>> = flow {
        emit(safeCall { remote.paymentMethods().map { it.toDomain() } })
    }
}
