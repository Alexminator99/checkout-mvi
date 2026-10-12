package com.example.core.mvi

import kotlinx.coroutines.flow.Flow

/**
 * Executes an effect and reports results back as actions.
 *
 * - One-shot work (a request) -> a Flow that emits one action.
 * - Subscriptions (observe a repository) -> a long-lived Flow.
 * - Fire-and-forget -> an empty Flow.
 *
 * Handlers must translate failures into actions; an exception escaping the Flow
 * is treated as a programming error and is not swallowed by the store.
 */
fun interface EffectHandler<E, A> {
    fun handle(effect: E): Flow<A>
}

/**
 * Concurrency policy, declared on the effect itself so the reducer stays in charge:
 * launching an effect whose [cancelKey] matches a running one cancels the old one
 * (latest-wins). Typical use: search-as-you-type, re-quoting, re-subscribing.
 */
interface KeyedEffect {
    val cancelKey: Any
}
