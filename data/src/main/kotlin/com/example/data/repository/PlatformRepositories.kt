package com.example.data.repository

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.domain.model.Customer
import com.example.domain.model.FeatureFlag
import com.example.domain.repository.CustomerRepository
import com.example.domain.repository.FeatureFlagRepository
import com.example.domain.repository.NetworkMonitor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

/** Android-specific adapter behind a domain port: the domain never sees ConnectivityManager. */
internal class ConnectivityNetworkMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) : NetworkMonitor {

    override val isOnline: Flow<Boolean> = callbackFlow {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(manager.activeNetwork != null) }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        manager.registerNetworkCallback(request, callback)
        trySend(manager.activeNetwork != null)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.conflate().distinctUntilChanged()
}

/** Session data source is whatever the app uses (DataStore, account manager, ...). */
interface SessionDataSource {
    val customer: Flow<Customer>
}

internal class SessionCustomerRepository @Inject constructor(
    private val session: SessionDataSource,
) : CustomerRepository {
    override fun observeCustomer(): Flow<Customer> = session.customer
}

/** Remote Config / LaunchDarkly / Statsig: hidden behind a tiny interface. */
interface RemoteFlagsDataSource {
    fun observeBoolean(key: String): Flow<Boolean>
}

internal class RemoteFeatureFlagRepository @Inject constructor(
    private val remote: RemoteFlagsDataSource,
) : FeatureFlagRepository {
    override fun observe(flag: FeatureFlag): Flow<Boolean> =
        remote.observeBoolean(flag.name.lowercase()).distinctUntilChanged()
}
