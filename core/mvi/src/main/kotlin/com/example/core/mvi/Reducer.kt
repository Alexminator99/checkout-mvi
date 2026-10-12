package com.example.core.mvi

/**
 * Pure state transition: (State, Action) -> Transition.
 *
 * Contract: no I/O, no coroutines, no clocks, no randomness, no Android types. The same input
 * always yields the same output. Side effects are described as [Transition.effects] and run by an
 * [EffectHandler]; one-off UI signals are described as [Transition.events].
 */
fun interface Reducer<State, Action, Effect, Event> {
    fun reduce(state: State, action: Action): Transition<State, Effect, Event>
}
