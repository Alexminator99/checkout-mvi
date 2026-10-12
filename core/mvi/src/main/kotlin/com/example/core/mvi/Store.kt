package com.example.core.mvi

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** The MVI loop: actions in, state + one-off events out. */
interface Store<S, A, V> {
    val state: StateFlow<S>
    val events: Flow<V>
    fun dispatch(action: A)
}

/**
 * Creates stores bound to a caller-owned scope (e.g. viewModelScope).
 * Injected as an abstraction so ViewModels never construct infrastructure themselves.
 */
interface StoreFactory {
    fun <S, A, E, V> create(
        initialState: S,
        scope: CoroutineScope,
        reducer: Reducer<S, A, E, V>,
        effectHandler: EffectHandler<E, A>,
    ): Store<S, A, V>
}
