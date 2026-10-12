package com.example.feature.checkout.data.repository

import com.example.feature.checkout.domain.model.FeatureFlag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoFeatureFlagRepositoryTest {

    @Test
    fun `express shipping is switched on in the demo`() = runTest {
        assertTrue(DemoFeatureFlagRepository().observe(FeatureFlag.EXPRESS_SHIPPING).first())
    }
}
