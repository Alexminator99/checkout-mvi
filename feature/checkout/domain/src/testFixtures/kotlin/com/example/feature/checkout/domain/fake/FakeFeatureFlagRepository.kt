package com.example.feature.checkout.domain.fake

import com.example.feature.checkout.domain.model.FeatureFlag
import com.example.feature.checkout.domain.repository.FeatureFlagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFeatureFlagRepository(enabled: Boolean = true) : FeatureFlagRepository {

    val expressShipping = MutableStateFlow(enabled)

    override fun observe(flag: FeatureFlag): Flow<Boolean> = expressShipping
}
