package com.example.feature.checkout.domain.fake

import com.example.feature.checkout.domain.repository.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeNetworkMonitor(online: Boolean = true) : NetworkMonitor {

    val online = MutableStateFlow(online)

    override val isOnline: Flow<Boolean> get() = online
}
