package com.example.feature.checkout.data.repository

import com.example.feature.checkout.domain.model.FeatureFlag
import com.example.feature.checkout.domain.repository.FeatureFlagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

/** Stands in for remote config: every flag of the demo is switched on. */
internal class DemoFeatureFlagRepository @Inject constructor() : FeatureFlagRepository {

    override fun observe(flag: FeatureFlag): Flow<Boolean> = flowOf(true)
}
