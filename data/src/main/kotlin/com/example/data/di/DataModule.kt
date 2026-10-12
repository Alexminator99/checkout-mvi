package com.example.data.di

import com.example.data.remote.CheckoutApi
import com.example.data.repository.AddressRepositoryImpl
import com.example.data.repository.CartRepositoryImpl
import com.example.data.repository.ConnectivityNetworkMonitor
import com.example.data.repository.OrderRepositoryImpl
import com.example.data.repository.PaymentMethodRepositoryImpl
import com.example.data.repository.PromoRepositoryImpl
import com.example.data.repository.RemoteFeatureFlagRepository
import com.example.data.repository.SessionCustomerRepository
import com.example.data.repository.ShippingRepositoryImpl
import com.example.domain.repository.AddressRepository
import com.example.domain.repository.CartReader
import com.example.domain.repository.CartWriter
import com.example.domain.repository.CustomerRepository
import com.example.domain.repository.FeatureFlagRepository
import com.example.domain.repository.NetworkMonitor
import com.example.domain.repository.OrderRepository
import com.example.domain.repository.PaymentMethodRepository
import com.example.domain.repository.PromoRepository
import com.example.domain.repository.ShippingRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import java.util.Currency
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

/**
 * The only place that knows which implementation backs which port.
 * Swap a backend (Retrofit -> GraphQL, Room -> SQLDelight) by changing this file
 * and :data; nothing in :domain or :feature recompiles against the change.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class RepositoryModule {

    // CartRepositoryImpl is @Singleton on the class, so both ports share one instance.
    @Binds abstract fun cartReader(impl: CartRepositoryImpl): CartReader
    @Binds abstract fun cartWriter(impl: CartRepositoryImpl): CartWriter

    @Binds @Singleton abstract fun addresses(impl: AddressRepositoryImpl): AddressRepository
    @Binds @Singleton abstract fun shipping(impl: ShippingRepositoryImpl): ShippingRepository
    @Binds @Singleton abstract fun promos(impl: PromoRepositoryImpl): PromoRepository
    @Binds @Singleton abstract fun payments(impl: PaymentMethodRepositoryImpl): PaymentMethodRepository
    @Binds @Singleton abstract fun orders(impl: OrderRepositoryImpl): OrderRepository
    @Binds @Singleton abstract fun customer(impl: SessionCustomerRepository): CustomerRepository
    @Binds @Singleton abstract fun network(impl: ConnectivityNetworkMonitor): NetworkMonitor
    @Binds @Singleton abstract fun flags(impl: RemoteFeatureFlagRepository): FeatureFlagRepository
}

@Module
@InstallIn(SingletonComponent::class)
internal object DataProvidesModule {

    @Provides @Singleton
    fun checkoutApi(retrofit: Retrofit): CheckoutApi = retrofit.create(CheckoutApi::class.java)

    @Provides @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides @Named("storeCurrency")
    fun storeCurrency(): Currency = Currency.getInstance("EUR")
}
