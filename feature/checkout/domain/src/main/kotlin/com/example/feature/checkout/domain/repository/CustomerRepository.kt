package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.model.Customer
import kotlinx.coroutines.flow.Flow

interface CustomerRepository {
    fun observeCustomer(): Flow<Customer>
}
