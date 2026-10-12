package com.example.feature.checkout.data.repository

import com.example.feature.checkout.domain.model.Customer
import com.example.feature.checkout.domain.repository.CustomerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** The signed-in customer of the demo: a member, so free standard shipping is visible. */
internal class DemoCustomerRepository @Inject constructor() : CustomerRepository {

    override fun observeCustomer(): Flow<Customer> = flowOf(DEMO_CUSTOMER)

    private companion object {
        val DEMO_CUSTOMER = Customer(id = "customer-1", displayName = "Alex", isMember = true)
    }
}
