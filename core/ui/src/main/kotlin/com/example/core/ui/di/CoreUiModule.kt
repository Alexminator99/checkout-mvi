package com.example.core.ui.di

import com.example.core.mvi.DefaultStoreFactory
import com.example.core.mvi.StoreFactory
import com.example.core.ui.LocaleMoneyFormatter
import com.example.core.ui.MoneyFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** :core:mvi has no DI dependency at all; it gets wired here. */
@Module
@InstallIn(SingletonComponent::class)
object CoreUiModule {

    @Provides @Singleton
    fun storeFactory(): StoreFactory = DefaultStoreFactory()

    @Provides @Singleton
    fun moneyFormatter(): MoneyFormatter = LocaleMoneyFormatter()
}
