package com.example.feature.checkout.data.di

import com.example.feature.checkout.data.network.ConnectivityNetworkMonitor
import com.example.feature.checkout.data.repository.AddressRepositoryImpl
import com.example.feature.checkout.data.repository.CartRepositoryImpl
import com.example.feature.checkout.data.repository.DemoCustomerRepository
import com.example.feature.checkout.data.repository.DemoFeatureFlagRepository
import com.example.feature.checkout.data.repository.OrderRepositoryImpl
import com.example.feature.checkout.data.repository.PaymentMethodRepositoryImpl
import com.example.feature.checkout.data.repository.PromoRepositoryImpl
import com.example.feature.checkout.data.repository.ShippingRepositoryImpl
import com.example.feature.checkout.domain.repository.AddressRepository
import com.example.feature.checkout.domain.repository.CartRepository
import com.example.feature.checkout.domain.repository.CustomerRepository
import com.example.feature.checkout.domain.repository.FeatureFlagRepository
import com.example.feature.checkout.domain.repository.NetworkMonitor
import com.example.feature.checkout.domain.repository.OrderRepository
import com.example.feature.checkout.domain.repository.PaymentMethodRepository
import com.example.feature.checkout.domain.repository.PromoRepository
import com.example.feature.checkout.domain.repository.ShippingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** The only place that knows which implementation backs each domain repository interface. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    @Binds
    abstract fun cartRepository(impl: CartRepositoryImpl): CartRepository

    @Binds
    abstract fun addressRepository(impl: AddressRepositoryImpl): AddressRepository

    @Binds
    abstract fun shippingRepository(impl: ShippingRepositoryImpl): ShippingRepository

    @Binds
    abstract fun promoRepository(impl: PromoRepositoryImpl): PromoRepository

    @Binds
    abstract fun paymentMethodRepository(impl: PaymentMethodRepositoryImpl): PaymentMethodRepository

    @Binds
    abstract fun orderRepository(impl: OrderRepositoryImpl): OrderRepository

    @Binds
    abstract fun customerRepository(impl: DemoCustomerRepository): CustomerRepository

    @Binds
    abstract fun networkMonitor(impl: ConnectivityNetworkMonitor): NetworkMonitor

    @Binds
    abstract fun featureFlagRepository(impl: DemoFeatureFlagRepository): FeatureFlagRepository
}
