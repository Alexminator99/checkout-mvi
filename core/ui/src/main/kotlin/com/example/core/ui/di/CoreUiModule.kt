package com.example.core.ui.di

import com.example.core.ui.LocaleMoneyFormatter
import com.example.core.ui.MoneyFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreUiModule {

    @Provides
    @Singleton
    fun moneyFormatter(): MoneyFormatter = LocaleMoneyFormatter()
}
