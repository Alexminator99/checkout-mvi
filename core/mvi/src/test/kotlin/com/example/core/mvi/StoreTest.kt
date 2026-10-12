package com.example.core.mvi

import app.cash.turbine.test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class StoreTest {

    private sealed interface TestAction {
        data class Add(val amount: Int) : TestAction
        data class Fetch(val id: Int) : TestAction
        data class Fetched(val id: Int) : TestAction
        data object Announce : TestAction
    }

    private data class TestEffect(val id: Int) : KeyedEffect {
        override val cancelKey: Any get() = "fetch"
    }

    private data class TestState(val total: Int = 0, val fetched: List<Int> = emptyList())

    private val reducer = Reducer<TestState, TestAction, TestEffect, String> { state, action ->
        when (action) {
            is TestAction.Add -> state.copy(total = state.total + action.amount).only()
            is TestAction.Fetch -> state.withEffects(TestEffect(action.id))
            is TestAction.Fetched -> state.copy(fetched = state.fetched + action.id).only()
            TestAction.Announce -> state.withEvent("hello")
        }
    }

    // The store loop must run on the test scheduler. advanceUntilIdle() stops as soon as only
    // backgroundScope work is left, so a store launched in backgroundScope would never run.
    private fun TestScope.storeScope(): CoroutineScope = CoroutineScope(StandardTestDispatcher(testScheduler))

    @Test
    fun `actions are reduced in dispatch order`() = runTest {
        val scope = storeScope()
        val store = Store(TestState(), scope, reducer) { emptyFlow() }

        repeat(100) { store.dispatch(TestAction.Add(1)) }
        advanceUntilIdle()

        assertEquals(100, store.state.value.total)
        scope.cancel()
    }

    @Test
    fun `effect results come back as actions`() = runTest {
        val scope = storeScope()
        val store = Store(TestState(), scope, reducer) { effect -> flowOf(TestAction.Fetched(effect.id)) }

        store.dispatch(TestAction.Fetch(7))
        advanceUntilIdle()

        assertEquals(listOf(7), store.state.value.fetched)
        scope.cancel()
    }

    @Test
    fun `a keyed effect cancels the running one with the same key`() = runTest {
        val gates = mapOf(1 to CompletableDeferred<Unit>(), 2 to CompletableDeferred<Unit>())
        val scope = storeScope()
        val store = Store(TestState(), scope, reducer) { effect ->
            flow {
                gates.getValue(effect.id).await()
                emit(TestAction.Fetched(effect.id))
            }
        }

        store.dispatch(TestAction.Fetch(1))
        advanceUntilIdle()
        store.dispatch(TestAction.Fetch(2))
        advanceUntilIdle()
        gates.values.forEach { it.complete(Unit) }
        advanceUntilIdle()

        assertEquals(listOf(2), store.state.value.fetched)
        scope.cancel()
    }

    @Test
    fun `events are delivered in order and never replayed`() = runTest {
        val scope = storeScope()
        val store = Store(TestState(), scope, reducer) { emptyFlow() }

        store.events.test {
            store.dispatch(TestAction.Announce)
            store.dispatch(TestAction.Announce)
            assertEquals("hello", awaitItem())
            assertEquals("hello", awaitItem())
        }
        store.events.test {
            expectNoEvents()
        }
        scope.cancel()
    }
}
