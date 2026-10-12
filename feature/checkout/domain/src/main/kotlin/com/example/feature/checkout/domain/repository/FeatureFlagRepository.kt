package com.example.feature.checkout.domain.repository

import com.example.feature.checkout.domain.model.FeatureFlag
import kotlinx.coroutines.flow.Flow

interface FeatureFlagRepository {
    fun observe(flag: FeatureFlag): Flow<Boolean>
}
