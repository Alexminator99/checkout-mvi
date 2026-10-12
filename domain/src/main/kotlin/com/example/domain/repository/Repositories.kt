package com.example.domain.repository

import com.example.domain.error.Outcome
import com.example.domain.model.Address
import com.example.domain.model.AddressValidation
import com.example.domain.model.Cart
import com.example.domain.model.Customer
import com.example.domain.model.FeatureFlag
import com.example.domain.model.Order
import com.example.domain.model.OrderRequest
import com.example.domain.model.PaymentMethod
import com.example.domain.model.Promo
import com.example.domain.model.ShippingOption
import kotlinx.coroutines.flow.Flow

// Ports. Owned by the domain, implemented by :data (Dependency Inversion).
// Kept small and role-specific (Interface Segregation): a use case that only
// reads the cart doesn't see write methods it doesn't need.

interface CartReader {
    fun observeCart(): Flow<Cart>
}

interface CartWriter {
    suspend fun setQuantity(sku: String, quantity: Int): Outcome<Unit>
    suspend fun removeLine(sku: String): Outcome<Unit>
}

// Remote-backed streams emit Outcome: a failed fetch is a value, not an exception
// that would tear down every collector downstream.

interface AddressRepository {
    fun observeAddresses(): Flow<Outcome<List<Address>>>
    suspend fun validate(addressId: String): Outcome<AddressValidation>
}

interface ShippingRepository {
    suspend fun quote(addressId: String, cart: Cart): Outcome<List<ShippingOption>>
}

interface PromoRepository {
    suspend fun redeem(code: String, cart: Cart): Outcome<Promo>
}

interface PaymentMethodRepository {
    fun observePaymentMethods(): Flow<Outcome<List<PaymentMethod>>>
}

interface OrderRepository {
    suspend fun place(request: OrderRequest): Outcome<Order>
}

interface CustomerRepository {
    fun observeCustomer(): Flow<Customer>
}

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

interface FeatureFlagRepository {
    fun observe(flag: FeatureFlag): Flow<Boolean>
}
