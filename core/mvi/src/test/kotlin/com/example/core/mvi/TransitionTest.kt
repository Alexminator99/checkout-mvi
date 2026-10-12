package com.example.core.mvi

import org.junit.Assert.assertEquals
import org.junit.Test

class TransitionTest {

    @Test
    fun `only carries no effects and no events`() {
        assertEquals(Transition<Int, Nothing, Nothing>(5), 5.only())
    }

    @Test
    fun `then applies the rule to the latest state and keeps everything requested`() {
        val transition: Transition<Int, String, String> = 1.withEffects("load")
            .then { state -> (state + 1).withEvent("loaded") }

        assertEquals(Transition(2, effects = listOf("load"), events = listOf("loaded")), transition)
    }

    @Test
    fun `plus keeps the latter state and concatenates effects and events`() {
        val first: Transition<String, Int, Int> = Transition("a", listOf(1), listOf(10))
        val second: Transition<String, Int, Int> = Transition("b", listOf(2), listOf(20))

        assertEquals(Transition("b", listOf(1, 2), listOf(10, 20)), first + second)
    }
}
