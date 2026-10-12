package com.example.core.mvi

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultStoreTest {

    // A tiny feature just for testing the loop.
    private sealed interface Act {
        data class Add(val n: Int) : Act
        data class Fetch(val id: Int) : Act
        data class Fetched(val id: Int) : Act
        data object Announce : Act
    }
    private data class Fx(val id: Int) : KeyedEffect { override val cancelKey: Any get() = "fetch" }
    private data class St(val total: Int = 0, val fetched: List<Int> = emptyList())

    private val reducer = Reducer<St, Act, Fx, String> { s, a ->
        when (a) {
            is Act.Add -> s.copy(total = s.total + a.n).only()
            is Act.Fetch -> s.withEffects(Fx(a.id))
            is Act.Fetched -> s.copy(fetched = s.fetched + a.id).only()
            Act.Announce -> s.withEvent("hello")
        }
    }

    @Test
    fun `actions are reduced in dispatch order`() = runTest {
        val store = DefaultStore(St(), backgroundScope, reducer) { emptyFlow() }

        repeat(100) { store.dispatch(Act.Add(1)) }
        advanceUntilIdle()

        assertEquals(100, store.state.value.total)
    }

    @Test
    fun `keyed effects are latest-wins`() = runTest {
        val gates = mapOf(1 to CompletableDeferred<Unit>(), 2 to CompletableDeferred<Unit>())
        val store = DefaultStore(St(), backgroundScope, reducer) { fx: Fx ->
            flow { gates.getValue(fx.id).await(); emit(Act.Fetched(fx.id)) }
        }

        store.dispatch(Act.Fetch(1))
        advanceUntilIdle()
        store.dispatch(Act.Fetch(2))          // cancels fetch 1
        advanceUntilIdle()
        gates.values.forEach { it.complete(Unit) }
        advanceUntilIdle()

        assertEquals(listOf(2), store.state.value.fetched)
    }

    @Test
    fun `events are delivered once`() = runTest {
        val store = DefaultStore(St(), backgroundScope, reducer) { emptyFlow() }
        val received = mutableListOf<String>()
        backgroundScope.launch { store.events.collect { received += it } }

        store.dispatch(Act.Announce)
        advanceUntilIdle()

        assertEquals(listOf("hello"), received)
    }
}
