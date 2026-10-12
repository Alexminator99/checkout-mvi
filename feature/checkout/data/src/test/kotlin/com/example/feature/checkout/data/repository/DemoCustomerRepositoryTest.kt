package com.example.feature.checkout.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoCustomerRepositoryTest {

    @Test
    fun `the demo customer is a member`() = runTest {
        assertTrue(DemoCustomerRepository().observeCustomer().first().isMember)
    }
}
