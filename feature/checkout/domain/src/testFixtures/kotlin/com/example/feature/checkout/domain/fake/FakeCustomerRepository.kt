package com.example.feature.checkout.domain.fake

import com.example.feature.checkout.domain.fixture.CUSTOMER
import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCustomerRepository(initial: Customer = CUSTOMER) : CustomerRepository {

    val customer = MutableStateFlow(initial)

    override fun observeCustomer(): Flow<Customer> = customer
}
