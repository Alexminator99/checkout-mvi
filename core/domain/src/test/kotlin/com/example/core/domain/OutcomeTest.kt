package com.example.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OutcomeTest {

    @Test
    fun `map transforms a success`() {
        val outcome: Outcome<Int, String> = Outcome.Success(2)

        assertEquals(Outcome.Success(4), outcome.map { it * 2 })
    }

    @Test
    fun `map keeps a failure`() {
        val outcome: Outcome<Int, String> = Outcome.Failure("boom")

        assertEquals(Outcome.Failure("boom"), outcome.map { it * 2 })
    }

    @Test
    fun `fold picks the matching branch`() {
        val success: Outcome<Int, String> = Outcome.Success(2)
        val failure: Outcome<Int, String> = Outcome.Failure("boom")

        assertEquals("value 2", success.fold({ "value $it" }, { "error $it" }))
        assertEquals("error boom", failure.fold({ "value $it" }, { "error $it" }))
    }
}
