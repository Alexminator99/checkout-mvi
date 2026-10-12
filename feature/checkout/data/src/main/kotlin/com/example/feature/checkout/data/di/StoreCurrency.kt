package com.example.feature.checkout.data.di

import javax.inject.Qualifier

/** The currency the shop sells in, used whenever the cart is empty and carries no currency of its own. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
internal annotation class StoreCurrency
