package com.example.feature.checkout.data.local

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCartDataSourceTest {

    private val source = InMemoryCartDataSource()

    @Test
    fun `starts with the demo cart`() = runTest {
        assertEquals(InMemoryCartDataSource.DEMO_LINES, source.observeLines().first())
    }

    @Test
    fun `updates a known line`() = runTest {
        assertTrue(source.updateQuantity("mug", 5))

        assertEquals(5, source.observeLines().first().first { it.sku == "mug" }.quantity)
    }

    @Test
    fun `deletes a known line`() = runTest {
        assertTrue(source.delete("mug"))

        assertEquals(listOf("tee"), source.observeLines().first().map { it.sku })
    }

    @Test
    fun `unknown lines are reported and nothing changes`() = runTest {
        assertFalse(source.updateQuantity("nope", 1))
        assertFalse(source.delete("nope"))
        assertEquals(InMemoryCartDataSource.DEMO_LINES, source.observeLines().first())
    }
}
