package com.example.feature.checkout.data.di

import com.example.feature.checkout.data.demo.DemoCheckoutData
import com.example.feature.checkout.data.local.CartDataSource
import com.example.feature.checkout.data.local.InMemoryCartDataSource
import com.example.feature.checkout.data.remote.CheckoutRemoteDataSource
import com.example.feature.checkout.data.remote.DemoCheckoutRemoteDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.Currency
import javax.inject.Singleton

/** Picks the data sources. Swapping the demo backend for a real API or Room is a change here only. */
@Module
@InstallIn(SingletonComponent::class)
internal object DataSourceModule {

    private const val DEMO_LATENCY_MILLIS = 600L

    @Provides
    @Singleton
    fun checkoutRemoteDataSource(): CheckoutRemoteDataSource = DemoCheckoutRemoteDataSource(DEMO_LATENCY_MILLIS)

    @Provides
    @Singleton
    fun cartDataSource(): CartDataSource = InMemoryCartDataSource()

    @Provides
    @StoreCurrency
    fun storeCurrency(): Currency = Currency.getInstance(DemoCheckoutData.CURRENCY)
}
