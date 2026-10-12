package com.example.core.mvi

/**
 * Pure state transition: (State, Action) -> Transition.
 *
 * Contract (enforced by review + tests, not by the compiler):
 *  - no I/O, no coroutines, no clocks, no randomness, no Android types
 *  - same input always yields the same output
 *
 * Side effects are *described* as [Transition.effects] and executed elsewhere
 * by an [EffectHandler]. One-off UI signals are described as [Transition.events].
 *
 * Note: no declaration-site variance on purpose. Kotlin turns `in`/`out` into
 * Java wildcards on constructor parameters, which makes Dagger/Hilt fail to
 * match `@Binds` return types. Keep injected generic interfaces invariant.
 */
fun interface Reducer<S, A, E, V> {
    fun reduce(state: S, action: A): Transition<S, E, V>
}
