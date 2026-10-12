package com.example.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Hilt aggregates every @InstallIn module on the classpath here: RepositoryModule
 * from :data, CoreUiModule from :core:ui, CheckoutModule from :feature:checkout.
 * App-specific bindings (Retrofit, Room database, SessionDataSource,
 * RemoteFlagsDataSource) live in this module too.
 */
@HiltAndroidApp
class ShopApp : Application()
