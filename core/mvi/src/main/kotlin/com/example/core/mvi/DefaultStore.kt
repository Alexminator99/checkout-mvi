package com.example.core.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Single-consumer MVI loop.
 *
 * Every action goes through one channel and is reduced by one coroutine, so:
 *  - actions are processed strictly in order
 *  - the reducer never runs concurrently with itself (no lost updates, no retries)
 *  - bookkeeping for [KeyedEffect] needs no locking
 *
 * The store never catches exceptions from effect handlers: handlers own error
 * translation, and anything that escapes is a bug that should surface loudly.
 */
class DefaultStore<S, A, E, V>(
    initialState: S,
    private val scope: CoroutineScope,
    private val reducer: Reducer<S, A, E, V>,
    private val effectHandler: EffectHandler<E, A>,
) : Store<S, A, V> {

    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<S> = _state.asStateFlow()

    private val _events = Channel<V>(Channel.BUFFERED)
    override val events: Flow<V> = _events.receiveAsFlow()

    private val actions = Channel<A>(Channel.UNLIMITED)
    private val keyedJobs = HashMap<Any, Job>()   // touched only by the consumer coroutine

    init {
        scope.launch {
            for (action in actions) process(action)
        }
    }

    override fun dispatch(action: A) {
        actions.trySend(action)   // UNLIMITED: never fails while the scope is alive
    }

    private suspend fun process(action: A) {
        val transition = reducer.reduce(_state.value, action)
        _state.value = transition.state
        transition.events.forEach { _events.send(it) }
        transition.effects.forEach(::launchEffect)
    }

    private fun launchEffect(effect: E) {
        val key = (effect as? KeyedEffect)?.cancelKey
        if (key != null) keyedJobs.remove(key)?.cancel()

        val job = scope.launch {
            effectHandler.handle(effect).collect { result -> dispatch(result) }
        }
        if (key != null) keyedJobs[key] = job
    }
}

/** Production [StoreFactory]. Bound in DI; replaceable in tests. */
class DefaultStoreFactory : StoreFactory {
    override fun <S, A, E, V> create(
        initialState: S,
        scope: CoroutineScope,
        reducer: Reducer<S, A, E, V>,
        effectHandler: EffectHandler<E, A>,
    ): Store<S, A, V> = DefaultStore(initialState, scope, reducer, effectHandler)
}
